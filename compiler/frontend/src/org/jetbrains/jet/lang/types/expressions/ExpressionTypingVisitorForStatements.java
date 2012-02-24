/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.Sets;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResultsUtil;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
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
            VariableDescriptor variableDescriptor = context.getDescriptorResolver()
                    .resolveObjectDeclaration(scope.getContainingDeclaration(), declaration, classDescriptor);
            scope.addVariableDescriptor(variableDescriptor);
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
            JetType outType = propertyDescriptor.getType();
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
        SimpleFunctionDescriptor functionDescriptor = context.getDescriptorResolver().resolveFunctionDescriptor(scope.getContainingDeclaration(), scope, function);
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
        //There is a temporary binding trace for an opportunity to resolve set method for array if needed (the initial trace should be used there)
        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(contextWithExpectedType.trace);
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceExpectedReturnType(TypeUtils.NO_EXPECTED_TYPE).replaceBindingTrace(temporaryBindingTrace);

        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
        if (left == null) return null;

        JetType leftType = facade.getType(left, context);
        if (leftType == null) {
            facade.getType(expression.getRight(), context);
            context.trace.report(UNRESOLVED_REFERENCE.on(operationSign));
            temporaryBindingTrace.commit();
            return null;
        }
        ExpressionReceiver receiver = new ExpressionReceiver(left, leftType);

        // We check that defined only one of '+=' and '+' operations, and call it (in the case '+' we then also assign)
        // Check for '+='
        String name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);
        TemporaryBindingTrace assignmentOperationTrace = TemporaryBindingTrace.create(context.trace);
        OverloadResolutionResults<FunctionDescriptor> assignmentOperationDescriptors = basic.getResolutionResultsForBinaryCall(scope, name, context.replaceBindingTrace(assignmentOperationTrace), expression, receiver);
        JetType assignmentOperationType = OverloadResolutionResultsUtil.getResultType(assignmentOperationDescriptors);

        // Check for '+'
        String counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));
        TemporaryBindingTrace binaryOperationTrace = TemporaryBindingTrace.create(context.trace);
        OverloadResolutionResults<FunctionDescriptor> binaryOperationDescriptors = basic.getResolutionResultsForBinaryCall(scope, counterpartName, context.replaceBindingTrace(binaryOperationTrace), expression, receiver);
        JetType binaryOperationType = OverloadResolutionResultsUtil.getResultType(binaryOperationDescriptors);

        JetType type = assignmentOperationType != null ? assignmentOperationType : binaryOperationType;
        if (assignmentOperationType != null && binaryOperationType != null) {
            OverloadResolutionResults<FunctionDescriptor> ambiguityResolutionResults = OverloadResolutionResultsUtil.ambiguity(assignmentOperationDescriptors, binaryOperationDescriptors);
            context.trace.report(ASSIGN_OPERATOR_AMBIGUITY.on(operationSign, ambiguityResolutionResults.getResultingCalls()));
            Collection<DeclarationDescriptor> descriptors = Sets.newHashSet();
            for (ResolvedCall<? extends FunctionDescriptor> call : ambiguityResolutionResults.getResultingCalls()) {
                descriptors.add(call.getResultingDescriptor());
            }
            facade.getType(expression.getRight(), context);
            context.trace.record(AMBIGUOUS_REFERENCE_TARGET, operationSign, descriptors);
        }
        else if (assignmentOperationType != null) {
            assignmentOperationTrace.commit();
            if (!JetStandardClasses.isUnit(assignmentOperationType)) {
                context.trace.report(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT.on(operationSign, assignmentOperationDescriptors.getResultingDescriptor(), operationSign));
            }
        } else {
            binaryOperationTrace.commit();
            context.trace.record(VARIABLE_REASSIGNMENT, expression);
            ExpressionTypingUtils.checkWrappingInRef(expression.getLeft(), context);
            if (left instanceof JetArrayAccessExpression) {
                ExpressionTypingContext contextForResolve = context.replaceScope(scope).replaceBindingTrace(TemporaryBindingTrace.create(contextWithExpectedType.trace));
                basic.resolveArrayAccessSetMethod((JetArrayAccessExpression) left, expression.getRight(), contextForResolve, context.trace);
            }
        }
        basic.checkLValue(context.trace, expression.getLeft());
        temporaryBindingTrace.commit();
        return checkAssignmentType(type, expression, contextWithExpectedType);
    }

    protected JetType visitAssignment(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceExpectedReturnType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
        JetExpression right = expression.getRight();
        if (left instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
            JetType assignmentType = basic.resolveArrayAccessSetMethod(arrayAccessExpression, right, context.replaceScope(scope), context.trace);
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

    @Override
    public JetType visitIdeTemplateExpression(JetIdeTemplateExpression expression, ExpressionTypingContext context) {
        context.trace.report(UNRESOLVED_IDE_TEMPLATE.on(expression, ObjectUtils.notNull(expression.getText(), "<no name>")));
        return null;
    }
}
