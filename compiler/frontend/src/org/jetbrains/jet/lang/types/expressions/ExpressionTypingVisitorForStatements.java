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
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.VARIABLE_REASSIGNMENT;

/**
 * @author abreslav
 */
public class ExpressionTypingVisitorForStatements extends ExpressionTypingVisitor {
    private final WritableScope scope;
    private final BasicExpressionTypingVisitor basic;
    private final ControlStructureTypingVisitor controlStructures;
    private final PatternMatchingTypingVisitor patterns;

    public ExpressionTypingVisitorForStatements(
            @NotNull ExpressionTypingInternals facade,
            @NotNull WritableScope scope,
            BasicExpressionTypingVisitor basic,
            @NotNull ControlStructureTypingVisitor controlStructures,
            @NotNull PatternMatchingTypingVisitor patterns) {
        super(facade);
        this.scope = scope;
        this.basic = basic;
        this.controlStructures = controlStructures;
        this.patterns = patterns;
    }

    @Nullable
    private JetType checkAssignmentType(@Nullable JetType assignmentType, @NotNull JetBinaryExpression expression, @NotNull ExpressionTypingContext context) {
        if (assignmentType != null && !JetStandardClasses.isUnit(assignmentType) && context.expectedType != TypeUtils.NO_EXPECTED_TYPE &&
            TypeUtils.equalTypes(context.expectedType, assignmentType)) {
            context.trace.report(Errors.ASSIGNMENT_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return DataFlowUtils.checkStatementType(expression, context);
    }

    @Override
    public JetType visitObjectDeclaration(JetObjectDeclaration declaration, ExpressionTypingContext context) {
        TopDownAnalyzer.processObject(context.semanticServices, context.trace, scope, scope.getContainingDeclaration(), declaration);
        ClassDescriptor classDescriptor = context.trace.getBindingContext().get(BindingContext.CLASS, declaration);
        if (classDescriptor != null) {
            PropertyDescriptor propertyDescriptor = context.getDescriptorResolver().resolveObjectDeclarationAsPropertyDescriptor(scope.getContainingDeclaration(), declaration, classDescriptor);
            scope.addPropertyDescriptor(propertyDescriptor);
        }
        return DataFlowUtils.checkStatementType(declaration, context);
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

        VariableDescriptor propertyDescriptor = context.getDescriptorResolver().resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, property, context.dataFlowInfo);
        JetExpression initializer = property.getInitializer();
        if (property.getPropertyTypeRef() != null && initializer != null) {
            JetType outType = propertyDescriptor.getOutType();
            JetType initializerType = facade.getType(initializer, context.replaceExpectedType(outType).replaceScope(scope));
        }
        
        {
            VariableDescriptor olderVariable = scope.getLocalVariable(propertyDescriptor.getName());
            if (olderVariable != null && DescriptorUtils.isLocal(propertyDescriptor.getContainingDeclaration(), olderVariable)) {
                context.trace.report(Errors.NAME_SHADOWING.on(propertyDescriptor, context.trace.getBindingContext()));
            }
        }

        scope.addVariableDescriptor(propertyDescriptor);
        return DataFlowUtils.checkStatementType(property, context);
    }

    @Override
    public JetType visitNamedFunction(JetNamedFunction function, ExpressionTypingContext context) {
        NamedFunctionDescriptor functionDescriptor = context.getDescriptorResolver().resolveFunctionDescriptor(scope.getContainingDeclaration(), scope, function);
        scope.addFunctionDescriptor(functionDescriptor);
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
        context.getServices().checkFunctionReturnType(functionInnerScope, function, functionDescriptor, context.dataFlowInfo);
        return DataFlowUtils.checkStatementType(function, context);
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
        return DataFlowUtils.checkStatementType(dcl, context);
    }

    @Override
    public JetType visitBinaryExpression(JetBinaryExpression expression, ExpressionTypingContext context) {
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        JetType result;
        if (operationType == JetTokens.EQ) {
            result = visitAssignment(expression, context);
        }
        else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, context);
        }
        else {
            return facade.getType(expression, context);
        }
        return DataFlowUtils.checkType(result, expression, context);
    }

    protected JetType visitAssignmentOperation(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceExpectedReturnType(TypeUtils.NO_EXPECTED_TYPE);
        // If there's += (or similar op) defined as such, we just call it.
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        String name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);

        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(context.trace);
        JetType assignmentOperationType = basic.getTypeForBinaryCall(scope, name, context.replaceBindingTrace(temporaryBindingTrace), expression);

        JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
        // If there isn't, we call plus (or like) and then assign
        if (assignmentOperationType == null) {
            String counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));

            if (left instanceof JetArrayAccessExpression) {
                JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
                basic.resolveArrayAccessToLValue(arrayAccessExpression, expression.getRight(), context.replaceScope(scope), false);
            }
            assignmentOperationType = basic.getTypeForBinaryCall(scope, counterpartName, context, expression);
            if (assignmentOperationType != null) {
                context.trace.record(VARIABLE_REASSIGNMENT, expression);
                ExpressionTypingUtils.checkWrappingInRef(expression.getLeft(), context);
            }
        }
        else {
            temporaryBindingTrace.commit();
            checkReassignment(expression, context, assignmentOperationType, left);
        }
        basic.checkLValue(context.trace, expression.getLeft());
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


    protected JetType visitAssignment(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceExpectedReturnType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
        JetExpression right = expression.getRight();
        if (left instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
            JetType assignmentType = basic.resolveArrayAccessToLValue(arrayAccessExpression, right, context.replaceScope(scope), false); //todo
            basic.checkLValue(context.trace, arrayAccessExpression);
            return checkAssignmentType(assignmentType, expression, contextWithExpectedType);
        }
        JetType leftType = facade.getType(expression.getLeft(), context.replaceScope(scope));
        if (right != null) {
            JetType rightType = facade.getType(right, context.replaceExpectedType(leftType).replaceScope(scope));
        }
        if (leftType != null) { //if leftType == null, some another error has been generated
            basic.checkLValue(context.trace, expression.getLeft());
        }
        if (left instanceof JetSimpleNameExpression) {
            ExpressionTypingUtils.checkWrappingInRef(left, context);
        }
        return DataFlowUtils.checkStatementType(expression, contextWithExpectedType);
    }


    @Override
    public JetType visitExpression(JetExpression expression, ExpressionTypingContext context) {
        return facade.getType(expression, context);
    }

    @Override
    public JetType visitJetElement(JetElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return null;
    }

    @Override
    public JetType visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitWhileExpression(expression, context, true);
    }

    @Override
    public JetType visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitDoWhileExpression(expression, context, true);
    }

    @Override
    public JetType visitForExpression(JetForExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitForExpression(expression, context, true);
    }

    @Override
    public JetType visitIfExpression(JetIfExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitIfExpression(expression, context, true);
    }

    @Override
    public JetType visitWhenExpression(final JetWhenExpression expression, ExpressionTypingContext context) {
        return patterns.visitWhenExpression(expression, context, true);
    }

    @Override
    public JetType visitBlockExpression(JetBlockExpression expression, ExpressionTypingContext context) {
        return basic.visitBlockExpression(expression, context, true);
    }

    @Override
    public JetType visitParenthesizedExpression(JetParenthesizedExpression expression, ExpressionTypingContext context) {
        return basic.visitParenthesizedExpression(expression, context, true);
    }

    @Override
    public JetType visitUnaryExpression(JetUnaryExpression expression, ExpressionTypingContext context) {
        return basic.visitUnaryExpression(expression, context, true);
    }
}
