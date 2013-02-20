/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorUtil;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.ModifiersChecker;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.VARIABLE_REASSIGNMENT;

@SuppressWarnings("SuspiciousMethodCalls")
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
        if (assignmentType != null && !KotlinBuiltIns.getInstance().isUnit(assignmentType) && context.expectedType != TypeUtils.NO_EXPECTED_TYPE &&
            TypeUtils.equalTypes(context.expectedType, assignmentType)) {
            context.trace.report(Errors.ASSIGNMENT_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return DataFlowUtils.checkStatementType(expression, context);
    }

    @Override
    public JetTypeInfo visitObjectDeclaration(JetObjectDeclaration declaration, ExpressionTypingContext context) {
        TopDownAnalyzer.processClassOrObject(context.expressionTypingServices.getProject(), context.trace, scope,
                                             scope.getContainingDeclaration(), declaration);
        ClassDescriptor classDescriptor = context.trace.getBindingContext().get(BindingContext.CLASS, declaration);
        if (classDescriptor != null) {
            VariableDescriptor variableDescriptor = context.expressionTypingServices.getDescriptorResolver()
                    .resolveObjectDeclaration(scope.getContainingDeclaration(), declaration, classDescriptor, context.trace);
            scope.addVariableDescriptor(variableDescriptor);
        }
        return DataFlowUtils.checkStatementType(declaration, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitProperty(JetProperty property, ExpressionTypingContext context) {
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

        VariableDescriptor propertyDescriptor = context.expressionTypingServices.getDescriptorResolver().
                resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, property, context.dataFlowInfo, context.trace);
        JetExpression initializer = property.getInitializer();
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (initializer != null) {
            JetType outType = propertyDescriptor.getType();
            JetTypeInfo typeInfo = facade.getTypeInfo(initializer, context.replaceExpectedType(outType).replaceScope(scope));
            dataFlowInfo = typeInfo.getDataFlowInfo();
        }
        
        {
            VariableDescriptor olderVariable = scope.getLocalVariable(propertyDescriptor.getName());
            ExpressionTypingUtils.checkVariableShadowing(context, propertyDescriptor, olderVariable);
        }

        scope.addVariableDescriptor(propertyDescriptor);
        ModifiersChecker.create(context.trace).checkModifiersForLocalDeclaration(property);
        return DataFlowUtils.checkStatementType(property, context, dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitMultiDeclaration(JetMultiDeclaration multiDeclaration, final ExpressionTypingContext context) {
        final JetExpression initializer = multiDeclaration.getInitializer();
        if (initializer == null) {
            context.trace.report(INITIALIZER_REQUIRED_FOR_MULTIDECLARATION.on(multiDeclaration));
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        final ExpressionReceiver expressionReceiver =
                ExpressionTypingUtils.getExpressionReceiver(facade, initializer, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
        DataFlowInfo dataFlowInfo = facade.getTypeInfo(initializer, context).getDataFlowInfo();
        if (expressionReceiver == null) {
            return JetTypeInfo.create(null, dataFlowInfo);
        }
        ExpressionTypingUtils.defineLocalVariablesFromMultiDeclaration(scope, multiDeclaration, expressionReceiver, initializer, context);
        return DataFlowUtils.checkStatementType(multiDeclaration, context, dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitNamedFunction(JetNamedFunction function, ExpressionTypingContext context) {
        SimpleFunctionDescriptor functionDescriptor = context.expressionTypingServices.getDescriptorResolver().
                resolveFunctionDescriptor(scope.getContainingDeclaration(), scope, function, context.trace);

        scope.addFunctionDescriptor(functionDescriptor);
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
        context.expressionTypingServices.checkFunctionReturnType(functionInnerScope, function, functionDescriptor, context.dataFlowInfo, null, context.trace);
        ModifiersChecker.create(context.trace).checkModifiersForLocalDeclaration(function);
        return DataFlowUtils.checkStatementType(function, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitClass(JetClass klass, ExpressionTypingContext context) {
        TopDownAnalyzer.processClassOrObject(context.expressionTypingServices.getProject(), context.trace, scope,
                                             scope.getContainingDeclaration(), klass);
        ClassDescriptor classDescriptor = context.trace.getBindingContext().get(BindingContext.CLASS, klass);
        if (classDescriptor != null) {
            scope.addClassifierDescriptor(classDescriptor);
        }
        return DataFlowUtils.checkStatementType(klass, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitTypedef(JetTypedef typedef, ExpressionTypingContext context) {
        return super.visitTypedef(typedef, context); // TODO
    }

    @Override
    public JetTypeInfo visitDeclaration(JetDeclaration dcl, ExpressionTypingContext context) {
        return DataFlowUtils.checkStatementType(dcl, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitBinaryExpression(JetBinaryExpression expression, ExpressionTypingContext context) {
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        JetTypeInfo result;
        if (operationType == JetTokens.EQ) {
            result = visitAssignment(expression, context);
        }
        else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, context);
        }
        else {
            return facade.getTypeInfo(expression, context);
        }
        return DataFlowUtils.checkType(result.getType(), expression, context, result.getDataFlowInfo());
    }

    @NotNull
    protected JetTypeInfo visitAssignmentOperation(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        //There is a temporary binding trace for an opportunity to resolve set method for array if needed (the initial trace should be used there)
        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(
                contextWithExpectedType.trace, "trace to resolve array set method for binary expression", expression);
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceBindingTrace(temporaryBindingTrace);

        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        JetTypeInfo leftInfo = facade.getTypeInfo(expression.getLeft(), context);
        JetType leftType = leftInfo.getType();
        DataFlowInfo dataFlowInfo = leftInfo.getDataFlowInfo();

        JetExpression right = expression.getRight();
        JetExpression left = JetPsiUtil.deparenthesizeWithNoTypeResolution(expression.getLeft());
        if (right == null || left == null) {
            temporaryBindingTrace.commit();
            return JetTypeInfo.create(null, dataFlowInfo);
        }

        if (leftType == null) {
            dataFlowInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo();
            context.trace.report(UNRESOLVED_REFERENCE.on(operationSign, operationSign));
            temporaryBindingTrace.commit();
            return JetTypeInfo.create(null, dataFlowInfo);
        }
        ExpressionReceiver receiver = new ExpressionReceiver(left, leftType);

        // We check that defined only one of '+=' and '+' operations, and call it (in the case '+' we then also assign)
        // Check for '+='
        Name name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);
        TemporaryBindingTrace assignmentOperationTrace = TemporaryBindingTrace.create(context.trace, "trace to check assignment operation like '+=' for", expression);
        OverloadResolutionResults<FunctionDescriptor> assignmentOperationDescriptors = basic.getResolutionResultsForBinaryCall(scope, name, context.replaceBindingTrace(assignmentOperationTrace), expression, receiver);
        JetType assignmentOperationType = OverloadResolutionResultsUtil.getResultType(assignmentOperationDescriptors);

        // Check for '+'
        Name counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));
        TemporaryBindingTrace binaryOperationTrace = TemporaryBindingTrace.create(context.trace, "trace to check binary operation like '+' for", expression);
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
            dataFlowInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo();
            context.trace.record(AMBIGUOUS_REFERENCE_TARGET, operationSign, descriptors);
        }
        else if (assignmentOperationType != null) {
            assignmentOperationTrace.commit();
            if (!KotlinBuiltIns.getInstance().isUnit(assignmentOperationType)) {
                context.trace.report(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT.on(operationSign, assignmentOperationDescriptors.getResultingDescriptor(), operationSign));
            }
        }
        else {
            binaryOperationTrace.commit();
            context.trace.record(VARIABLE_REASSIGNMENT, expression);
            if (left instanceof JetArrayAccessExpression) {
                ExpressionTypingContext contextForResolve = context.replaceScope(scope).replaceBindingTrace(TemporaryBindingTrace.create(
                        contextWithExpectedType.trace, "trace to resolve array set method for assignment", expression));
                basic.resolveArrayAccessSetMethod((JetArrayAccessExpression) left, right, contextForResolve, context.trace);
            }
            dataFlowInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo();
        }
        basic.checkLValue(context.trace, expression.getLeft());
        temporaryBindingTrace.commit();
        return JetTypeInfo.create(checkAssignmentType(type, expression, contextWithExpectedType), dataFlowInfo);
    }

    @NotNull
    protected JetTypeInfo visitAssignment(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceScope(scope);
        JetExpression left = context.expressionTypingServices.deparenthesize(expression.getLeft(), context);
        JetExpression right = expression.getRight();
        if (left instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
            if (right == null) return JetTypeInfo.create(null, context.dataFlowInfo);
            JetTypeInfo typeInfo = basic.resolveArrayAccessSetMethod(arrayAccessExpression, right, context, context.trace);
            basic.checkLValue(context.trace, arrayAccessExpression);
            return JetTypeInfo.create(checkAssignmentType(typeInfo.getType(), expression, contextWithExpectedType),
                                      typeInfo.getDataFlowInfo());
        }
        JetTypeInfo leftInfo = facade.getTypeInfo(expression.getLeft(), context);
        JetType leftType = leftInfo.getType();
        DataFlowInfo dataFlowInfo = leftInfo.getDataFlowInfo();
        if (right != null) {
            JetTypeInfo rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo).replaceExpectedType(leftType));
            dataFlowInfo = rightInfo.getDataFlowInfo();
        }
        if (leftType != null) { //if leftType == null, some another error has been generated
            basic.checkLValue(context.trace, expression.getLeft());
        }
        return DataFlowUtils.checkStatementType(expression, contextWithExpectedType, dataFlowInfo);
    }


    @Override
    public JetTypeInfo visitExpression(JetExpression expression, ExpressionTypingContext context) {
        return facade.getTypeInfo(expression, context);
    }

    @Override
    public JetTypeInfo visitJetElement(JetElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitWhileExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitDoWhileExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitForExpression(JetForExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitForExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitIfExpression(JetIfExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitIfExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitWhenExpression(final JetWhenExpression expression, ExpressionTypingContext context) {
        return patterns.visitWhenExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitBlockExpression(JetBlockExpression expression, ExpressionTypingContext context) {
        return basic.visitBlockExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitParenthesizedExpression(JetParenthesizedExpression expression, ExpressionTypingContext context) {
        return basic.visitParenthesizedExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitUnaryExpression(JetUnaryExpression expression, ExpressionTypingContext context) {
        return basic.visitUnaryExpression(expression, context, true);
    }
}
