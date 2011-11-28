package org.jetbrains.jet.lang.types.expressions;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.INDEXED_LVALUE_SET;
import static org.jetbrains.jet.lang.resolve.BindingContext.VARIABLE_REASSIGNMENT;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.getExpressionReceiver;

/**
 * @author abreslav
 */
public class ExpressionTypingVisitorForStatements extends BasicExpressionTypingVisitor {
    private final WritableScope scope;

    public ExpressionTypingVisitorForStatements(@NotNull ExpressionTypingInternals facade, @NotNull WritableScope scope) {
        super(facade);
        this.scope = scope;
    }

    @Nullable
    private JetType checkExpectedType(@NotNull JetExpression expression, @NotNull ExpressionTypingContext context) {
        if (context.expectedType != TypeUtils.NO_EXPECTED_TYPE) {
            if (JetStandardClasses.isUnit(context.expectedType)) {
                return JetStandardClasses.getUnitType();
            }
            context.trace.report(EXPECTED_TYPE_MISMATCH.on(expression, context.expectedType));
        }
        return null;
    }

    @Nullable
    private JetType checkAssignmentType(@Nullable JetType assignmentType, @NotNull JetBinaryExpression expression, @NotNull ExpressionTypingContext context) {
        if (assignmentType != null && !JetStandardClasses.isUnit(assignmentType) && context.expectedType != TypeUtils.NO_EXPECTED_TYPE &&
            TypeUtils.equalTypes(context.expectedType, assignmentType)) {
            context.trace.report(Errors.ASSIGNMENT_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return checkExpectedType(expression, context);
    }

    @Override
    public JetType visitObjectDeclaration(JetObjectDeclaration declaration, ExpressionTypingContext context) {
        TopDownAnalyzer.processObject(context.semanticServices, context.trace, scope, scope.getContainingDeclaration(), declaration);
        ClassDescriptor classDescriptor = context.trace.getBindingContext().get(BindingContext.CLASS, declaration);
        if (classDescriptor != null) {
            PropertyDescriptor propertyDescriptor = context.getDescriptorResolver().resolveObjectDeclarationAsPropertyDescriptor(scope.getContainingDeclaration(), declaration, classDescriptor);
            scope.addVariableDescriptor(propertyDescriptor);
        }
        return checkExpectedType(declaration, context);
    }

    @Override
    public JetType visitProperty(JetProperty property, ExpressionTypingContext context) {
        JetTypeReference receiverTypeRef = property.getReceiverTypeRef();
        if (receiverTypeRef != null) {
            context.trace.report(LOCAL_EXTENSION_PROPERTY.on(receiverTypeRef));
        }

        JetPropertyAccessor getter = property.getGetter();
        if (getter != null) {
            context.trace.report(LOCAL_VARIABLE_WITH_GETTER.on(getter));
        }

        JetPropertyAccessor setter = property.getSetter();
        if (setter != null) {
            context.trace.report(LOCAL_VARIABLE_WITH_SETTER.on(setter));
        }

        VariableDescriptor propertyDescriptor = context.getDescriptorResolver().resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, property);
        JetExpression initializer = property.getInitializer();
        if (property.getPropertyTypeRef() != null && initializer != null) {
            JetType outType = propertyDescriptor.getOutType();
            JetType initializerType = facade.getType(initializer, context.replaceExpectedType(outType).replaceScope(scope));
        }
        
        {
            VariableDescriptor olderVariable = scope.getVariable(propertyDescriptor.getName());
            if (olderVariable != null && DescriptorUtils.isLocal(propertyDescriptor.getContainingDeclaration(), olderVariable)) {
                context.trace.report(Errors.NAME_SHADOWING.on(propertyDescriptor, context.trace.getBindingContext()));
            }
        }

        scope.addVariableDescriptor(propertyDescriptor);
        return checkExpectedType(property, context);
    }

    @Override
    public JetType visitNamedFunction(JetNamedFunction function, ExpressionTypingContext context) {
        FunctionDescriptorImpl functionDescriptor = context.getDescriptorResolver().resolveFunctionDescriptor(scope.getContainingDeclaration(), scope, function);
        scope.addFunctionDescriptor(functionDescriptor);
        context.getServices().checkFunctionReturnType(context.scope, function, functionDescriptor, context.dataFlowInfo);
        return checkExpectedType(function, context);
    }

    @Override
    public JetType visitClass(JetClass klass, ExpressionTypingContext context) {
        return super.visitClass(klass, context); // TODO
    }

    @Override
    public JetType visitTypedef(JetTypedef typedef, ExpressionTypingContext context) {
        return super.visitTypedef(typedef, context); // TODO
    }

    @Override
    public JetType visitDeclaration(JetDeclaration dcl, ExpressionTypingContext context) {
        return checkExpectedType(dcl, context);
    }

    @Override
    protected JetType visitAssignmentOperation(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceExpectedReturnType(TypeUtils.NO_EXPECTED_TYPE);
        // If there's += (or similar op) defined as such, we just call it.
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        String name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);

        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(context.trace);
        JetType assignmentOperationType = getTypeForBinaryCall(scope, name, context.replaceBindingTrace(temporaryBindingTrace), expression);

        JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
        // If there isn't, we call plus (or like) and then assign
        if (assignmentOperationType == null) {
            String counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));

            if (left instanceof JetArrayAccessExpression) {
                JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
                resolveArrayAccessToLValue(arrayAccessExpression, expression.getRight(), operationSign, context);
            }
            assignmentOperationType = getTypeForBinaryCall(scope, counterpartName, context, expression);
            if (assignmentOperationType != null) {
                context.trace.record(VARIABLE_REASSIGNMENT, expression);
                ExpressionTypingUtils.checkWrappingInRef(expression.getLeft(), context);
            }
        }
        else {
            temporaryBindingTrace.commit();
            checkReassignment(expression, context, assignmentOperationType, left);
        }
        checkLValue(context.trace, expression.getLeft());
        return checkAssignmentType(assignmentOperationType, expression, contextWithExpectedType);
    }

    private void checkReassignment(@NotNull JetBinaryExpression expression, @NotNull ExpressionTypingContext context,
                                   @NotNull JetType assignmentOperationType, @Nullable JetExpression left) {
        if (left == null) return;
        JetType leftType = facade.getType(left, context.replaceScope(scope));
        if (leftType == null) return;
        boolean isUnit = context.semanticServices.getTypeChecker().
                isSubtypeOf(assignmentOperationType, JetStandardClasses.getUnitType());
        if (isUnit) return;

        if (context.semanticServices.getTypeChecker().isSubtypeOf(assignmentOperationType, leftType)) {
            context.trace.record(VARIABLE_REASSIGNMENT, expression);
        }
        else {
            context.trace.report(TYPE_MISMATCH.on(expression, leftType, assignmentOperationType));
        }
    }

    @Override
    protected JetType visitAssignment(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceExpectedReturnType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
        JetExpression right = expression.getRight();
        if (left instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
            JetType assignmentType = resolveArrayAccessToLValue(arrayAccessExpression, right, expression.getOperationReference(), context);
            checkLValue(context.trace, arrayAccessExpression);
            return checkAssignmentType(assignmentType, expression, contextWithExpectedType);
        }
        JetType leftType = facade.getType(expression.getLeft(), context.replaceScope(scope));
        if (right != null) {
            JetType rightType = facade.getType(right, context.replaceExpectedType(leftType).replaceScope(scope));
        }
        if (leftType != null) { //if leftType == null, some another error has been generated
            checkLValue(context.trace, expression.getLeft());
        }
        if (left instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression simpleName = (JetSimpleNameExpression) left;
            String referencedName = simpleName.getReferencedName();
            if (simpleName.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER
                && referencedName != null) {
                PropertyDescriptor property = context.scope.getPropertyByFieldReference(referencedName);
                if (property != null) {
                    context.trace.record(BindingContext.VARIABLE_ASSIGNMENT, simpleName, property);
                }
            }
            ExpressionTypingUtils.checkWrappingInRef(simpleName, context);
        }
        return checkExpectedType(expression, contextWithExpectedType);
    }

    private JetType resolveArrayAccessToLValue(JetArrayAccessExpression arrayAccessExpression, JetExpression rightHandSide, JetSimpleNameExpression operationSign, ExpressionTypingContext context) {
        ExpressionReceiver receiver = getExpressionReceiver(facade, arrayAccessExpression.getArrayExpression(), context.replaceScope(scope));
        if (receiver == null) return null;

        Call call = CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide);
        ResolvedCall<FunctionDescriptor> setFunctionCall = context.replaceScope(scope).replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).resolveCallWithGivenName(
                call,
                arrayAccessExpression,
                "set");
        if (setFunctionCall == null) return null;
        FunctionDescriptor setFunctionDescriptor = setFunctionCall.getResultingDescriptor();

        context.trace.record(INDEXED_LVALUE_SET, arrayAccessExpression, setFunctionCall);

//        if (getterNeeded) {
//            ResolvedCall<FunctionDescriptor> getFunctionCall = context.replaceScope(scope).replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).resolveCallWithGivenName(
//                    CallMaker.makeArrayGetCall(receiver, arrayAccessExpression),
//                    arrayAccessExpression,
//                    "get");
//            if (getFunctionCall == null) return null;
//            context.trace.record(INDEXED_LVALUE_GET, arrayAccessExpression, getFunctionCall);
//        }
//        else {
//            context.trace.record(REFERENCE_TARGET, operationSign, setFunctionDescriptor);
//        }
        return setFunctionDescriptor.getReturnType();
    }

    @Override
    public JetType visitJetElement(JetElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return null;
    }
}
