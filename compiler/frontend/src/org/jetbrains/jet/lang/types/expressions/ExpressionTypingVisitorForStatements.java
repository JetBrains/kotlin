package org.jetbrains.jet.lang.types.expressions;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.INDEXED_LVALUE_GET;
import static org.jetbrains.jet.lang.resolve.BindingContext.INDEXED_LVALUE_SET;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
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
    private JetType checkExpectedType(JetExpression expression, ExpressionTypingContext context) {
        if (context.expectedType != TypeUtils.NO_EXPECTED_TYPE) {
            if (JetStandardClasses.isUnit(context.expectedType)) {
                return JetStandardClasses.getUnitType();
            }
            context.trace.report(EXPECTED_TYPE_MISMATCH.on(expression, context.expectedType));
        }
        return null;
    }

    @Override
    public JetType visitObjectDeclaration(JetObjectDeclaration declaration, ExpressionTypingContext context) {
        TopDownAnalyzer.processObject(context.semanticServices, context.trace, scope, scope.getContainingDeclaration(), declaration);
        ClassDescriptor classDescriptor = context.trace.getBindingContext().get(BindingContext.CLASS, declaration);
        if (classDescriptor != null) {
            PropertyDescriptor propertyDescriptor = context.getClassDescriptorResolver().resolveObjectDeclarationAsPropertyDescriptor(scope.getContainingDeclaration(), declaration, classDescriptor);
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

        VariableDescriptor propertyDescriptor = context.getClassDescriptorResolver().resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, property);
        JetExpression initializer = property.getInitializer();
        if (property.getPropertyTypeRef() != null && initializer != null) {
            JetType outType = propertyDescriptor.getOutType();
            JetType initializerType = facade.getType(initializer, context.replaceExpectedType(outType).replaceScope(scope));
        }

        scope.addVariableDescriptor(propertyDescriptor);
        return checkExpectedType(property, context);
    }

    @Override
    public JetType visitNamedFunction(JetNamedFunction function, ExpressionTypingContext context) {
        FunctionDescriptorImpl functionDescriptor = context.getClassDescriptorResolver().resolveFunctionDescriptor(scope.getContainingDeclaration(), scope, function);
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
    protected JetType visitAssignmentOperation(JetBinaryExpression expression, ExpressionTypingContext context) {
        // If there's += (or similar op) defined as such, we just call it.
        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
        String name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);

        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(context.trace);
        JetType assignmentOperationType = getTypeForBinaryCall(scope, name, context.replaceBindingTrace(temporaryBindingTrace), expression);

        // If there isn't, we call plus (or like) and then assign
        if (assignmentOperationType == null) {
            String counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));

            JetType typeForBinaryCall = getTypeForBinaryCall(scope, counterpartName, context, expression);
            if (typeForBinaryCall != null) {
                JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
                if (left instanceof JetArrayAccessExpression) {
                    resolveArrayAccessToLValue((JetArrayAccessExpression) left, expression.getRight(), expression.getOperationReference(), context, true);
                }

                context.trace.record(BindingContext.VARIABLE_REASSIGNMENT, expression);
                ExpressionTypingUtils.checkWrappingInRef(expression.getLeft(), context);
            }
        }
        else {
            temporaryBindingTrace.commit();
        }
        return checkExpectedType(expression, context);
    }

    @Override
    protected JetType visitAssignment(JetBinaryExpression expression, ExpressionTypingContext context) {
        boolean getterNeeded = false;
        JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
        JetExpression right = expression.getRight();
        if (left instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
            return resolveArrayAccessToLValue(arrayAccessExpression, right, expression.getOperationReference(), context, getterNeeded);
        }
        JetType leftType = facade.getType(left, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceScope(scope));
        if (right != null) {
            JetType rightType = facade.getType(right, context.replaceExpectedType(leftType).replaceScope(scope));
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
        return checkExpectedType(expression, context);
    }

    private JetType resolveArrayAccessToLValue(JetArrayAccessExpression arrayAccessExpression, JetExpression rightHandSide, JetSimpleNameExpression operationSign, ExpressionTypingContext context, boolean getterNeeded) {
        ExpressionReceiver receiver = getExpressionReceiver(facade, arrayAccessExpression.getArrayExpression(), context.replaceScope(scope));
        if (receiver == null) return null;

        Call call = CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide);
        FunctionDescriptor setFunction = context.replaceScope(scope).replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).resolveCallWithGivenName(
                call,
                arrayAccessExpression,
                "set", receiver);
        if (setFunction == null) return null;
        context.trace.record(INDEXED_LVALUE_SET, arrayAccessExpression, setFunction);

        if (getterNeeded) {
            FunctionDescriptor getFunction = context.replaceScope(scope).replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).resolveCallWithGivenName(
                    CallMaker.makeArrayGetCall(receiver, arrayAccessExpression),
                    arrayAccessExpression,
                    "get", receiver);
            if (getFunction == null) return null;
            context.trace.record(INDEXED_LVALUE_GET, arrayAccessExpression, getFunction);
        }
        else {
            context.trace.record(REFERENCE_TARGET, operationSign, setFunction);
        }
        return DataFlowUtils.checkType(setFunction.getReturnType(), arrayAccessExpression, context);
    }

    @Override
    public JetType visitJetElement(JetElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return null;
    }
}
