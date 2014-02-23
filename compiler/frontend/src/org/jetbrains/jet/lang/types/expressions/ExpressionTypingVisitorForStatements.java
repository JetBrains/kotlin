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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.evaluate.EvaluatePackage;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.TemporaryTraceAndCache;
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
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.jet.lang.types.TypeUtils.noExpectedType;

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
    private static JetType checkAssignmentType(
            @Nullable JetType assignmentType,
            @NotNull JetBinaryExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        if (assignmentType != null && !KotlinBuiltIns.getInstance().isUnit(assignmentType) && !noExpectedType(context.expectedType) &&
            TypeUtils.equalTypes(context.expectedType, assignmentType)) {
            context.trace.report(Errors.ASSIGNMENT_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return DataFlowUtils.checkStatementType(expression, context);
    }

    @Override
    public JetTypeInfo visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, ExpressionTypingContext context) {
        TopDownAnalyzer.processClassOrObject(
                components.globalContext,
                scope, context.replaceScope(scope).replaceContextDependency(INDEPENDENT), scope.getContainingDeclaration(), declaration);
        return DataFlowUtils.checkStatementType(declaration, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitProperty(@NotNull JetProperty property, ExpressionTypingContext typingContext) {
        ExpressionTypingContext context = typingContext.replaceContextDependency(INDEPENDENT).replaceScope(scope);
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

        JetExpression delegateExpression = property.getDelegateExpression();
        if (delegateExpression != null) {
            components.expressionTypingServices.getTypeInfo(delegateExpression, context);
            context.trace.report(LOCAL_VARIABLE_WITH_DELEGATE.on(property.getDelegate()));
        }

        for (JetTypeParameter typeParameter : property.getTypeParameters()) {
            AnnotationResolver.reportUnsupportedAnnotationForTypeParameter(typeParameter, context.trace);
        }

        VariableDescriptor propertyDescriptor = components.expressionTypingServices.getDescriptorResolver().
                resolveLocalVariableDescriptor(scope, property, context.dataFlowInfo, context.trace);
        JetExpression initializer = property.getInitializer();
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (initializer != null) {
            JetType outType = propertyDescriptor.getType();
            JetTypeInfo typeInfo = facade.getTypeInfo(initializer, context.replaceExpectedType(outType));
            dataFlowInfo = typeInfo.getDataFlowInfo();

            EvaluatePackage.recordCompileTimeValueForInitializerIfNeeded(propertyDescriptor, initializer, outType, context.trace);
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
    public JetTypeInfo visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration, ExpressionTypingContext context) {
        components.expressionTypingServices.getAnnotationResolver().resolveAnnotationsWithArguments(
                scope, multiDeclaration.getModifierList(), context.trace);

        JetExpression initializer = multiDeclaration.getInitializer();
        if (initializer == null) {
            context.trace.report(INITIALIZER_REQUIRED_FOR_MULTIDECLARATION.on(multiDeclaration));
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        ExpressionReceiver expressionReceiver = ExpressionTypingUtils.getExpressionReceiver(
                facade, initializer, context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT));
        DataFlowInfo dataFlowInfo = facade.getTypeInfo(initializer, context).getDataFlowInfo();
        if (expressionReceiver == null) {
            return JetTypeInfo.create(null, dataFlowInfo);
        }
        components.expressionTypingUtils.defineLocalVariablesFromMultiDeclaration(scope, multiDeclaration, expressionReceiver, initializer, context);
        return DataFlowUtils.checkStatementType(multiDeclaration, context, dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitNamedFunction(@NotNull JetNamedFunction function, ExpressionTypingContext context) {
        SimpleFunctionDescriptor functionDescriptor = components.expressionTypingServices.getDescriptorResolver().
                resolveFunctionDescriptorWithAnnotationArguments(
                        scope.getContainingDeclaration(), scope, function, context.trace, context.dataFlowInfo);

        scope.addFunctionDescriptor(functionDescriptor);
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
        components.expressionTypingServices.checkFunctionReturnType(functionInnerScope, function, functionDescriptor, context.dataFlowInfo, null, context.trace);

        components.expressionTypingServices.resolveValueParameters(function.getValueParameters(), functionDescriptor.getValueParameters(),
                                                                scope, context.dataFlowInfo, context.trace, /* needCompleteAnalysis = */ true);

        ModifiersChecker.create(context.trace).checkModifiersForLocalDeclaration(function);
        return DataFlowUtils.checkStatementType(function, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitClass(@NotNull JetClass klass, ExpressionTypingContext context) {
        TopDownAnalyzer.processClassOrObject(
                components.globalContext,
                scope, context.replaceScope(scope).replaceContextDependency(INDEPENDENT), scope.getContainingDeclaration(), klass);
        return DataFlowUtils.checkStatementType(klass, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitTypedef(@NotNull JetTypedef typedef, ExpressionTypingContext context) {
        return super.visitTypedef(typedef, context); // TODO
    }

    @Override
    public JetTypeInfo visitDeclaration(@NotNull JetDeclaration dcl, ExpressionTypingContext context) {
        return DataFlowUtils.checkStatementType(dcl, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitBinaryExpression(@NotNull JetBinaryExpression expression, ExpressionTypingContext context) {
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
        TemporaryTraceAndCache temporary = TemporaryTraceAndCache.create(
                contextWithExpectedType, "trace to resolve array set method for binary expression", expression);
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceTraceAndCache(temporary).replaceContextDependency(INDEPENDENT);

        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        JetExpression leftOperand = expression.getLeft();
        JetTypeInfo leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(leftOperand, context, facade);
        JetType leftType = leftInfo.getType();
        DataFlowInfo dataFlowInfo = leftInfo.getDataFlowInfo();

        JetExpression right = expression.getRight();
        JetExpression left = leftOperand == null ? null : JetPsiUtil.deparenthesize(leftOperand);
        if (right == null || left == null) {
            temporary.commit();
            return JetTypeInfo.create(null, dataFlowInfo);
        }

        if (leftType == null) {
            dataFlowInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo();
            context.trace.report(UNRESOLVED_REFERENCE.on(operationSign, operationSign));
            temporary.commit();
            return JetTypeInfo.create(null, dataFlowInfo);
        }
        ExpressionReceiver receiver = new ExpressionReceiver(left, leftType);

        // We check that defined only one of '+=' and '+' operations, and call it (in the case '+' we then also assign)
        // Check for '+='
        Name name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);
        TemporaryTraceAndCache temporaryForAssignmentOperation = TemporaryTraceAndCache.create(
                context, "trace to check assignment operation like '+=' for", expression);
        OverloadResolutionResults<FunctionDescriptor> assignmentOperationDescriptors =
                components.callResolver.resolveBinaryCall(
                        context.replaceTraceAndCache(temporaryForAssignmentOperation).replaceScope(scope),
                        receiver, expression, name
                );
        JetType assignmentOperationType = OverloadResolutionResultsUtil.getResultingType(assignmentOperationDescriptors,
                                                                                         context.contextDependency);

        // Check for '+'
        Name counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));
        TemporaryTraceAndCache temporaryForBinaryOperation = TemporaryTraceAndCache.create(
                context, "trace to check binary operation like '+' for", expression);
        OverloadResolutionResults<FunctionDescriptor> binaryOperationDescriptors = components.callResolver.resolveBinaryCall(
                context.replaceTraceAndCache(temporaryForBinaryOperation).replaceScope(scope),
                receiver, expression, counterpartName
        );
        JetType binaryOperationType = OverloadResolutionResultsUtil.getResultingType(binaryOperationDescriptors, context.contextDependency);

        JetType type = assignmentOperationType != null ? assignmentOperationType : binaryOperationType;
        if (assignmentOperationDescriptors.isSuccess() && binaryOperationDescriptors.isSuccess()) {
            // Both 'plus()' and 'plusAssign()' available => ambiguity
            OverloadResolutionResults<FunctionDescriptor> ambiguityResolutionResults = OverloadResolutionResultsUtil.ambiguity(assignmentOperationDescriptors, binaryOperationDescriptors);
            context.trace.report(ASSIGN_OPERATOR_AMBIGUITY.on(operationSign, ambiguityResolutionResults.getResultingCalls()));
            Collection<DeclarationDescriptor> descriptors = Sets.newHashSet();
            for (ResolvedCall<? extends FunctionDescriptor> call : ambiguityResolutionResults.getResultingCalls()) {
                descriptors.add(call.getResultingDescriptor());
            }
            dataFlowInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo();
            context.trace.record(AMBIGUOUS_REFERENCE_TARGET, operationSign, descriptors);
        }
        else if (assignmentOperationType != null && (assignmentOperationDescriptors.isSuccess() || !binaryOperationDescriptors.isSuccess())) {
            // There's 'plusAssign()', so we do a.plusAssign(b)
            temporaryForAssignmentOperation.commit();
            if (!KotlinBuiltIns.getInstance().isUnit(assignmentOperationType)) {
                context.trace.report(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT.on(operationSign, assignmentOperationDescriptors.getResultingDescriptor(), operationSign));
            }
        }
        else {
            // There's only 'plus()', so we try 'a = a + b'
            temporaryForBinaryOperation.commit();
            context.trace.record(VARIABLE_REASSIGNMENT, expression);
            if (left instanceof JetArrayAccessExpression) {
                ExpressionTypingContext contextForResolve = context.replaceScope(scope).replaceBindingTrace(TemporaryBindingTrace.create(
                        context.trace, "trace to resolve array set method for assignment", expression));
                basic.resolveArrayAccessSetMethod((JetArrayAccessExpression) left, right, contextForResolve, context.trace);
            }
            dataFlowInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo();
            BasicExpressionTypingVisitor.checkLValue(context.trace, leftOperand);
        }
        temporary.commit();
        return JetTypeInfo.create(checkAssignmentType(type, expression, contextWithExpectedType), dataFlowInfo);
    }

    @NotNull
    protected JetTypeInfo visitAssignment(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceScope(scope).replaceContextDependency(INDEPENDENT);
        JetExpression leftOperand = expression.getLeft();
        JetExpression left = components.expressionTypingServices.deparenthesizeWithTypeResolution(leftOperand, context);
        JetExpression right = expression.getRight();
        if (left instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
            if (right == null) return JetTypeInfo.create(null, context.dataFlowInfo);
            JetTypeInfo typeInfo = basic.resolveArrayAccessSetMethod(arrayAccessExpression, right, context, context.trace);
            BasicExpressionTypingVisitor.checkLValue(context.trace, arrayAccessExpression);
            return JetTypeInfo.create(checkAssignmentType(typeInfo.getType(), expression, contextWithExpectedType),
                                      typeInfo.getDataFlowInfo());
        }
        JetTypeInfo leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade);
        JetType leftType = leftInfo.getType();
        DataFlowInfo dataFlowInfo = leftInfo.getDataFlowInfo();
        if (right != null) {
            JetTypeInfo rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo).replaceExpectedType(leftType));
            dataFlowInfo = rightInfo.getDataFlowInfo();
        }
        if (leftType != null && leftOperand != null) { //if leftType == null, some other error has been generated
            BasicExpressionTypingVisitor.checkLValue(context.trace, leftOperand);
        }
        return DataFlowUtils.checkStatementType(expression, contextWithExpectedType, dataFlowInfo);
    }


    @Override
    public JetTypeInfo visitExpression(@NotNull JetExpression expression, ExpressionTypingContext context) {
        return facade.getTypeInfo(expression, context);
    }

    @Override
    public JetTypeInfo visitJetElement(@NotNull JetElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitWhileExpression(@NotNull JetWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitWhileExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(@NotNull JetDoWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitDoWhileExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitForExpression(@NotNull JetForExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitForExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitAnnotatedExpression(
            @NotNull JetAnnotatedExpression expression, ExpressionTypingContext data
    ) {
        return basic.visitAnnotatedExpression(expression, data, true);
    }

    @Override
    public JetTypeInfo visitIfExpression(@NotNull JetIfExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitIfExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitWhenExpression(@NotNull JetWhenExpression expression, ExpressionTypingContext context) {
        return patterns.visitWhenExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitBlockExpression(@NotNull JetBlockExpression expression, ExpressionTypingContext context) {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, true);
    }

    @Override
    public JetTypeInfo visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, ExpressionTypingContext context) {
        return basic.visitParenthesizedExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitUnaryExpression(@NotNull JetUnaryExpression expression, ExpressionTypingContext context) {
        return basic.visitUnaryExpression(expression, context, true);
    }
}
