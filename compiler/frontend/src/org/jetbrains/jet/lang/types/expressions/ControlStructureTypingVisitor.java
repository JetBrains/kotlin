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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory1;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.*;

public class ControlStructureTypingVisitor extends ExpressionTypingVisitor {

    protected ControlStructureTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @NotNull
    private DataFlowInfo checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition, ExpressionTypingContext context) {
        if (condition != null) {
            JetTypeInfo typeInfo = facade.getTypeInfo(condition, context.replaceScope(scope));
            JetType conditionType = typeInfo.getType();

            if (conditionType != null && !isBoolean(conditionType)) {
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(condition, conditionType));
            }

            return typeInfo.getDataFlowInfo();
        }
        return context.dataFlowInfo;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public JetTypeInfo visitIfExpression(JetIfExpression expression, ExpressionTypingContext context) {
        return visitIfExpression(expression, context, false);
    }

    public JetTypeInfo visitIfExpression(JetIfExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        DataFlowInfo conditionDataFlowInfo = checkCondition(context.scope, condition, context);

        JetExpression elseBranch = expression.getElse();
        JetExpression thenBranch = expression.getThen();

        WritableScopeImpl thenScope = newWritableScopeImpl(context, "Then scope");
        WritableScopeImpl elseScope = newWritableScopeImpl(context, "Else scope");
        DataFlowInfo thenInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, context).and(conditionDataFlowInfo);
        DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, context).and(conditionDataFlowInfo);

        if (elseBranch == null) {
            if (thenBranch != null) {
                JetTypeInfo typeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(thenScope, Collections.singletonList(thenBranch), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(thenInfo), context.trace);
                JetType type = typeInfo.getType();
                DataFlowInfo dataFlowInfo;
                if (type != null && KotlinBuiltIns.getInstance().isNothing(type)) {
                    dataFlowInfo = elseInfo;
                } else {
                    dataFlowInfo = typeInfo.getDataFlowInfo().or(elseInfo);
                }
                return DataFlowUtils.checkImplicitCast(DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getUnitType(), expression, contextWithExpectedType), expression, contextWithExpectedType, isStatement, dataFlowInfo);
            }
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        if (thenBranch == null) {
            JetTypeInfo typeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(elseScope, Collections.singletonList(elseBranch), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(elseInfo), context.trace);
            JetType type = typeInfo.getType();
            DataFlowInfo dataFlowInfo;
            if (type != null && KotlinBuiltIns.getInstance().isNothing(type)) {
                dataFlowInfo = thenInfo;
            } else {
                dataFlowInfo = typeInfo.getDataFlowInfo().or(thenInfo);
            }
            return DataFlowUtils.checkImplicitCast(DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getUnitType(), expression, contextWithExpectedType), expression, contextWithExpectedType, isStatement, dataFlowInfo);
        }
        CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
        JetTypeInfo thenTypeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(thenScope, Collections.singletonList(thenBranch), coercionStrategy, contextWithExpectedType.replaceDataFlowInfo(thenInfo), context.trace);
        JetTypeInfo elseTypeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(elseScope, Collections.singletonList(elseBranch), coercionStrategy, contextWithExpectedType.replaceDataFlowInfo(elseInfo), context.trace);
        JetType thenType = thenTypeInfo.getType();
        JetType elseType = elseTypeInfo.getType();
        DataFlowInfo thenDataFlowInfo = thenTypeInfo.getDataFlowInfo();
        DataFlowInfo elseDataFlowInfo = elseTypeInfo.getDataFlowInfo();

        boolean jumpInThen = thenType != null && KotlinBuiltIns.getInstance().isNothing(thenType);
        boolean jumpInElse = elseType != null && KotlinBuiltIns.getInstance().isNothing(elseType);

        JetTypeInfo result;
        if (thenType == null && elseType == null) {
            result = JetTypeInfo.create(null, thenDataFlowInfo.or(elseDataFlowInfo));
        }
        else if (thenType == null || (jumpInThen && !jumpInElse)) {
            result = elseTypeInfo;
        }
        else if (elseType == null || (jumpInElse && !jumpInThen)) {
            result = thenTypeInfo;
        }
        else {
            result = JetTypeInfo.create(CommonSupertypes.commonSupertype(Arrays.asList(thenType, elseType)), thenDataFlowInfo.or(elseDataFlowInfo));
        }

        return DataFlowUtils.checkImplicitCast(result.getType(), expression, contextWithExpectedType, isStatement, result.getDataFlowInfo());
     }

    @Override
    public JetTypeInfo visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext context) {
        return visitWhileExpression(expression, context, false);
    }

    public JetTypeInfo visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        DataFlowInfo dataFlowInfo = checkCondition(context.scope, condition, context);

        JetExpression body = expression.getBody();
        if (body != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context, "Scope extended in while's condition");
            DataFlowInfo conditionInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, context).and(dataFlowInfo);
            context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(scopeToExtend, Collections.singletonList(body), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(conditionInfo), context.trace);
        }

        if (!containsJumpOutOfLoop(expression, context)) {
            dataFlowInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, context).and(dataFlowInfo);
        }
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getUnitType(), expression, contextWithExpectedType, dataFlowInfo);
    }

    private boolean containsJumpOutOfLoop(final JetLoopExpression loopExpression, final ExpressionTypingContext context) {
        final boolean[] result = new boolean[1];
        result[0] = false;
        //todo breaks in inline function literals
        loopExpression.accept(new JetTreeVisitor<List<JetLoopExpression>>() {
            @Override
            public Void visitBreakExpression(JetBreakExpression breakExpression, List<JetLoopExpression> outerLoops) {
                JetSimpleNameExpression targetLabel = breakExpression.getTargetLabel();
                PsiElement element = targetLabel != null ? context.trace.get(LABEL_TARGET, targetLabel) : null;
                if (element == loopExpression || (targetLabel == null && outerLoops.get(outerLoops.size() - 1) == loopExpression)) {
                    result[0] = true;
                }
                return null;
            }

            @Override
            public Void visitContinueExpression(JetContinueExpression expression, List<JetLoopExpression> outerLoops) {
                // continue@someOuterLoop is also considered as break
                JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                if (targetLabel != null) {
                    PsiElement element = context.trace.get(LABEL_TARGET, targetLabel);
                    if (element instanceof JetLoopExpression && !outerLoops.contains(element)) {
                        result[0] = true;
                    }
                }
                return null;
            }

            @Override
            public Void visitLoopExpression(JetLoopExpression loopExpression, List<JetLoopExpression> outerLoops) {
                List<JetLoopExpression> newOuterLoops = Lists.newArrayList(outerLoops);
                newOuterLoops.add(loopExpression);
                return super.visitLoopExpression(loopExpression, newOuterLoops);
            }
        }, Lists.newArrayList(loopExpression));

        return result[0];
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext context) {
        return visitDoWhileExpression(expression, context, false);
    }
    public JetTypeInfo visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression body = expression.getBody();
        JetScope conditionScope = context.scope;
        if (body instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression function = (JetFunctionLiteralExpression) body;
            if (!function.getFunctionLiteral().hasParameterSpecification()) {
                WritableScope writableScope = newWritableScopeImpl(context, "do..while body scope");
                conditionScope = writableScope;
                context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(writableScope, function.getFunctionLiteral().getBodyExpression().getStatements(), CoercionStrategy.NO_COERCION, context, context.trace);
                context.trace.record(BindingContext.BLOCK, function);
            }
            else {
                facade.getTypeInfo(body, context.replaceScope(context.scope));
            }
        }
        else if (body != null) {
            WritableScope writableScope = newWritableScopeImpl(context, "do..while body scope");
            conditionScope = writableScope;
            List<JetElement> block;
            if (body instanceof JetBlockExpression) {
                block = ((JetBlockExpression)body).getStatements();
            }
            else {
                block = Collections.<JetElement>singletonList(body);
            }
            context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(writableScope, block, CoercionStrategy.NO_COERCION, context, context.trace);
        }
        JetExpression condition = expression.getCondition();
        DataFlowInfo conditionDataFlowInfo = checkCondition(conditionScope, condition, context);
        DataFlowInfo dataFlowInfo;
        if (!containsJumpOutOfLoop(expression, context)) {
            dataFlowInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, context).and(conditionDataFlowInfo);
        }
        else {
            dataFlowInfo = context.dataFlowInfo;
        }
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getUnitType(), expression, contextWithExpectedType, dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitForExpression(JetForExpression expression, ExpressionTypingContext context) {
        return visitForExpression(expression, context, false);
    }

    public JetTypeInfo visitForExpression(JetForExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression loopRange = expression.getLoopRange();
        JetType expectedParameterType = null;
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (loopRange != null) {
            ExpressionReceiver loopRangeReceiver = getExpressionReceiver(facade, loopRange, context.replaceScope(context.scope));
            dataFlowInfo = facade.getTypeInfo(loopRange, context).getDataFlowInfo();
            if (loopRangeReceiver != null) {
                expectedParameterType = checkIterableConvention(loopRangeReceiver, context);
            }
        }

        WritableScope loopScope = newWritableScopeImpl(context, "Scope with for-loop index");

        JetParameter loopParameter = expression.getLoopParameter();
        if (loopParameter != null) {
            VariableDescriptor variableDescriptor = createLoopParameterDescriptor(loopParameter, expectedParameterType, context);

            loopScope.addVariableDescriptor(variableDescriptor);
        }
        else {
            JetMultiDeclaration multiParameter = expression.getMultiParameter();
            if (multiParameter != null && loopRange != null) {
                JetType elementType = expectedParameterType == null ? ErrorUtils.createErrorType("Loop range has no type") : expectedParameterType;
                TransientReceiver iteratorNextAsReceiver = new TransientReceiver(elementType);
                ExpressionTypingUtils.defineLocalVariablesFromMultiDeclaration(loopScope, multiParameter, iteratorNextAsReceiver, loopRange, context);
            }
        }

        JetExpression body = expression.getBody();
        if (body != null) {
            context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(loopScope, Collections.singletonList(body),
                    CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(dataFlowInfo), context.trace);
        }

        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getUnitType(), expression, contextWithExpectedType, dataFlowInfo);
    }

    private static VariableDescriptor createLoopParameterDescriptor(
            JetParameter loopParameter,
            JetType expectedParameterType,
            ExpressionTypingContext context
    ) {
        DescriptorResolver.checkParameterHasNoValOrVar(context.trace, loopParameter, VAL_OR_VAR_ON_LOOP_PARAMETER);

        JetTypeReference typeReference = loopParameter.getTypeReference();
        VariableDescriptor variableDescriptor;
        if (typeReference != null) {
            variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, loopParameter, context.trace);
            JetType actualParameterType = variableDescriptor.getType();
            if (expectedParameterType != null &&
                    !JetTypeChecker.INSTANCE.isSubtypeOf(expectedParameterType, actualParameterType)) {
                context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType));
            }
        }
        else {
            if (expectedParameterType == null) {
                expectedParameterType = ErrorUtils.createErrorType("Error");
            }
            variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), loopParameter, expectedParameterType, context.trace);
        }

        {
            // http://youtrack.jetbrains.net/issue/KT-527

            VariableDescriptor olderVariable = context.scope.getLocalVariable(variableDescriptor.getName());
            if (olderVariable != null && DescriptorUtils.isLocal(context.scope.getContainingDeclaration(), olderVariable)) {
                PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context.trace.getBindingContext(), variableDescriptor);
                context.trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().getName()));
            }
        }
        return variableDescriptor;
    }

    @Nullable
    /*package*/ static JetType checkIterableConvention(@NotNull ExpressionReceiver loopRange, ExpressionTypingContext context) {
        JetExpression loopRangeExpression = loopRange.getExpression();

        // Make a fake call loopRange.iterator(), and try to resolve it
        Name iterator = Name.identifier("iterator");
        Pair<Call, OverloadResolutionResults<FunctionDescriptor>> calls = makeAndResolveFakeCall(loopRange, context, Collections.<JetExpression>emptyList(), iterator);
        Call iteratorCall = calls.getFirst();
        OverloadResolutionResults<FunctionDescriptor> iteratorResolutionResults = calls.getSecond();

        if (iteratorResolutionResults.isSuccess()) {
            ResolvedCall<FunctionDescriptor> iteratorResolvedCall = iteratorResolutionResults.getResultingCall();
            context.trace.record(LOOP_RANGE_ITERATOR_RESOLVED_CALL, loopRangeExpression, iteratorResolvedCall);
            context.trace.record(LOOP_RANGE_ITERATOR_CALL, loopRangeExpression, iteratorCall);

            FunctionDescriptor iteratorFunction = iteratorResolvedCall.getResultingDescriptor();
            JetType iteratorType = iteratorFunction.getReturnType();
            JetType hasNextType = checkConventionForIterator(context, loopRangeExpression, iteratorType, "hasNext",
                                                             HAS_NEXT_FUNCTION_AMBIGUITY, HAS_NEXT_MISSING, HAS_NEXT_FUNCTION_NONE_APPLICABLE,
                                                             LOOP_RANGE_HAS_NEXT_RESOLVED_CALL);
            if (hasNextType != null && !isBoolean(hasNextType)) {
                context.trace.report(HAS_NEXT_FUNCTION_TYPE_MISMATCH.on(loopRangeExpression, hasNextType));
            }
            return checkConventionForIterator(context, loopRangeExpression, iteratorType, "next",
                                              NEXT_AMBIGUITY, NEXT_MISSING, NEXT_NONE_APPLICABLE,
                                              LOOP_RANGE_NEXT_RESOLVED_CALL);
        }
        else {
            if (iteratorResolutionResults.isAmbiguity()) {
//                    StringBuffer stringBuffer = new StringBuffer("Method 'iterator()' is ambiguous for this expression: ");
//                    for (FunctionDescriptor functionDescriptor : iteratorResolutionResults.getResultingCalls()) {
//                        stringBuffer.append(DescriptorRendererImpl.TEXT.render(functionDescriptor)).append(" ");
//                    }
//                    errorMessage = stringBuffer.toString();
                context.trace.report(ITERATOR_AMBIGUITY.on(loopRangeExpression, iteratorResolutionResults.getResultingCalls()));
            }
            else {
                context.trace.report(ITERATOR_MISSING.on(loopRangeExpression));
            }
        }
        return null;
    }

    @Nullable
    private static JetType checkConventionForIterator(
            @NotNull ExpressionTypingContext context,
            @NotNull JetExpression loopRangeExpression,
            @NotNull JetType iteratorType,
            @NotNull String name,
            @NotNull DiagnosticFactory1<JetExpression, JetType> ambiguity,
            @NotNull DiagnosticFactory1<JetExpression, JetType> missing,
            @NotNull DiagnosticFactory1<JetExpression, JetType> noneApplicable,
            @NotNull WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> resolvedCallKey
    ) {
        OverloadResolutionResults<FunctionDescriptor> nextResolutionResults = resolveFakeCall(
                context, new TransientReceiver(iteratorType), Name.identifier(name));
        if (nextResolutionResults.isAmbiguity()) {
            context.trace.report(ambiguity.on(loopRangeExpression, iteratorType));
        }
        else if (nextResolutionResults.isNothing()) {
            context.trace.report(missing.on(loopRangeExpression, iteratorType));
        }
        else if (!nextResolutionResults.isSuccess()) {
            context.trace.report(noneApplicable.on(loopRangeExpression, iteratorType));
        }
        else {
            assert nextResolutionResults.isSuccess();
            ResolvedCall<FunctionDescriptor> resolvedCall = nextResolutionResults.getResultingCall();
            context.trace.record(resolvedCallKey, loopRangeExpression, resolvedCall);
            return resolvedCall.getResultingDescriptor().getReturnType();
        }
        return null;
    }

    @Override
    public JetTypeInfo visitTryExpression(JetTryExpression expression, ExpressionTypingContext context) {
        JetExpression tryBlock = expression.getTryBlock();
        List<JetCatchClause> catchClauses = expression.getCatchClauses();
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        List<JetType> types = new ArrayList<JetType>();
        for (JetCatchClause catchClause : catchClauses) {
            JetParameter catchParameter = catchClause.getCatchParameter();
            JetExpression catchBody = catchClause.getCatchBody();
            if (catchParameter != null) {
                DescriptorResolver.checkParameterHasNoValOrVar(context.trace, catchParameter, VAL_OR_VAR_ON_CATCH_PARAMETER);

                VariableDescriptor variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(
                        context.scope.getContainingDeclaration(), context.scope, catchParameter, context.trace);
                JetType throwableType = KotlinBuiltIns.getInstance().getThrowable().getDefaultType();
                DataFlowUtils.checkType(variableDescriptor.getType(), catchParameter, context.replaceExpectedType(throwableType));
                if (catchBody != null) {
                    WritableScope catchScope = newWritableScopeImpl(context, "Catch scope");
                    catchScope.addVariableDescriptor(variableDescriptor);
                    JetType type = facade.getTypeInfo(catchBody, context.replaceScope(catchScope)).getType();
                    if (type != null) {
                        types.add(type);
                    }
                }
            }
        }

        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (finallyBlock != null) {
            dataFlowInfo = facade.getTypeInfo(finallyBlock.getFinalExpression(),
                                              context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)).getDataFlowInfo();
        }

        JetType type = facade.getTypeInfo(tryBlock, context).getType();
        if (type != null) {
            types.add(type);
        }
        if (types.isEmpty()) {
            return JetTypeInfo.create(null, dataFlowInfo);
        }
        else {
            return JetTypeInfo.create(CommonSupertypes.commonSupertype(types), dataFlowInfo);
        }
    }

    @Override
    public JetTypeInfo visitThrowExpression(JetThrowExpression expression, ExpressionTypingContext context) {
        JetExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            JetType throwableType = KotlinBuiltIns.getInstance().getThrowable().getDefaultType();
            facade.getTypeInfo(thrownExpression, context.replaceExpectedType(throwableType).replaceScope(context.scope));
        }
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getNothingType(), expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitReturnExpression(JetReturnExpression expression, ExpressionTypingContext context) {
        JetElement element = context.labelResolver.resolveLabel(expression, context);

        JetExpression returnedExpression = expression.getReturnedExpression();

        JetType expectedType = TypeUtils.NO_EXPECTED_TYPE;
        JetExpression parentDeclaration = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);
        if (parentDeclaration instanceof JetFunctionLiteral) {
            parentDeclaration = (JetFunctionLiteralExpression) parentDeclaration.getParent();
        }
        if (parentDeclaration instanceof JetParameter) {
            context.trace.report(RETURN_NOT_ALLOWED.on(expression));
        }
        assert parentDeclaration != null;
        DeclarationDescriptor declarationDescriptor = context.trace.get(DECLARATION_TO_DESCRIPTOR, parentDeclaration);
        FunctionDescriptor containingFunctionDescriptor = DescriptorUtils.getParentOfType(declarationDescriptor, FunctionDescriptor.class, false);

        if (expression.getTargetLabel() == null) {
            if (containingFunctionDescriptor != null) {
                PsiElement containingFunction = BindingContextUtils.callableDescriptorToDeclaration(context.trace.getBindingContext(), containingFunctionDescriptor);
                assert containingFunction != null;
                if (containingFunction instanceof JetFunctionLiteralExpression) {
                    do {
                        containingFunctionDescriptor = DescriptorUtils.getParentOfType(containingFunctionDescriptor, FunctionDescriptor.class);
                        containingFunction = containingFunctionDescriptor != null ? BindingContextUtils.callableDescriptorToDeclaration(context.trace.getBindingContext(), containingFunctionDescriptor) : null;
                    } while (containingFunction instanceof JetFunctionLiteralExpression);
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                }
                if (containingFunctionDescriptor != null) {
                    expectedType = DescriptorUtils.getFunctionExpectedReturnType(containingFunctionDescriptor, (JetElement) containingFunction);
                }
            }
            else {
                context.trace.report(RETURN_NOT_ALLOWED.on(expression));
            }
        }
        else if (element != null) {
            SimpleFunctionDescriptor functionDescriptor = context.trace.get(FUNCTION, element);
            if (functionDescriptor != null) {
                expectedType = DescriptorUtils.getFunctionExpectedReturnType(functionDescriptor, element);
                if (functionDescriptor != containingFunctionDescriptor) {
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                }
            }
            else {
                context.trace.report(NOT_A_RETURN_LABEL.on(expression, expression.getLabelName()));
            }
        }
        if (returnedExpression != null) {
            facade.getTypeInfo(returnedExpression, context.replaceExpectedType(expectedType).replaceScope(context.scope));
        }
        else {
            if (expectedType != TypeUtils.NO_EXPECTED_TYPE && expectedType != null && !KotlinBuiltIns.getInstance().isUnit(expectedType)) {
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, expectedType));
            }
        }
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getNothingType(), expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitBreakExpression(JetBreakExpression expression, ExpressionTypingContext context) {
        context.labelResolver.resolveLabel(expression, context);
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getNothingType(), expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitContinueExpression(JetContinueExpression expression, ExpressionTypingContext context) {
        context.labelResolver.resolveLabel(expression, context);
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getNothingType(), expression, context, context.dataFlowInfo);
    }
}
