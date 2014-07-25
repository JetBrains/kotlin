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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.jet.lang.types.TypeUtils.noExpectedType;
import static org.jetbrains.jet.lang.types.expressions.ControlStructureTypingUtils.createCallForSpecialConstruction;
import static org.jetbrains.jet.lang.types.expressions.ControlStructureTypingUtils.createDataFlowInfoForArgumentsForIfCall;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.*;

public class ControlStructureTypingVisitor extends ExpressionTypingVisitor {

    public static final String RETURN_NOT_ALLOWED_MESSAGE = "Return not allowed";

    protected ControlStructureTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @NotNull
    private DataFlowInfo checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition, ExpressionTypingContext context) {
        if (condition != null) {
            JetTypeInfo typeInfo = facade.getTypeInfo(condition, context.replaceScope(scope)
                    .replaceExpectedType(KotlinBuiltIns.getInstance().getBooleanType()).replaceContextDependency(INDEPENDENT));
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
    public JetTypeInfo visitIfExpression(@NotNull JetIfExpression expression, ExpressionTypingContext context) {
        return visitIfExpression(expression, context, false);
    }

    public JetTypeInfo visitIfExpression(JetIfExpression ifExpression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
        JetExpression condition = ifExpression.getCondition();
        DataFlowInfo conditionDataFlowInfo = checkCondition(context.scope, condition, context);

        JetExpression elseBranch = ifExpression.getElse();
        JetExpression thenBranch = ifExpression.getThen();

        WritableScopeImpl thenScope = newWritableScopeImpl(context, "Then scope");
        WritableScopeImpl elseScope = newWritableScopeImpl(context, "Else scope");
        DataFlowInfo thenInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, context).and(conditionDataFlowInfo);
        DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, context).and(conditionDataFlowInfo);

        if (elseBranch == null) {
            if (thenBranch != null) {
                return getTypeInfoWhenOnlyOneBranchIsPresent(
                        thenBranch, thenScope, thenInfo, elseInfo, contextWithExpectedType, ifExpression, isStatement);
            }
            return DataFlowUtils.checkImplicitCast(KotlinBuiltIns.getInstance().getUnitType(), ifExpression, contextWithExpectedType,
                                                   isStatement, thenInfo.or(elseInfo));
        }
        if (thenBranch == null) {
            return getTypeInfoWhenOnlyOneBranchIsPresent(
                    elseBranch, elseScope, elseInfo, thenInfo, contextWithExpectedType, ifExpression, isStatement);
        }
        JetPsiFactory psiFactory = JetPsiFactory(ifExpression);
        JetBlockExpression thenBlock = psiFactory.wrapInABlock(thenBranch);
        JetBlockExpression elseBlock = psiFactory.wrapInABlock(elseBranch);
        Call callForIf = createCallForSpecialConstruction(ifExpression, ifExpression, Lists.newArrayList(thenBlock, elseBlock));
        MutableDataFlowInfoForArguments dataFlowInfoForArguments =
                    createDataFlowInfoForArgumentsForIfCall(callForIf, thenInfo, elseInfo);
        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                callForIf, "If", Lists.newArrayList("thenBranch", "elseBranch"),
                Lists.newArrayList(false, false),
                contextWithExpectedType, dataFlowInfoForArguments);

        JetTypeInfo thenTypeInfo = BindingContextUtils.getRecordedTypeInfo(thenBranch, context.trace.getBindingContext());
        JetTypeInfo elseTypeInfo = BindingContextUtils.getRecordedTypeInfo(elseBranch, context.trace.getBindingContext());
        assert thenTypeInfo != null : "'Then' branch of if expression  was not processed: " + ifExpression;
        assert elseTypeInfo != null : "'Else' branch of if expression  was not processed: " + ifExpression;

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
            result = JetTypeInfo.create(resolvedCall.getResultingDescriptor().getReturnType(), thenDataFlowInfo.or(elseDataFlowInfo));
        }

        return DataFlowUtils.checkImplicitCast(result.getType(), ifExpression, contextWithExpectedType, isStatement, result.getDataFlowInfo());
    }

    @NotNull
    private JetTypeInfo getTypeInfoWhenOnlyOneBranchIsPresent(
            @NotNull JetExpression presentBranch,
            @NotNull WritableScopeImpl presentScope,
            @NotNull DataFlowInfo presentInfo,
            @NotNull DataFlowInfo otherInfo,
            @NotNull ExpressionTypingContext context,
            @NotNull JetIfExpression ifExpression,
            boolean isStatement
    ) {
        ExpressionTypingContext newContext = context.replaceDataFlowInfo(presentInfo).replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(INDEPENDENT);
        JetTypeInfo typeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                presentScope, Collections.singletonList(presentBranch), CoercionStrategy.NO_COERCION, newContext, context.trace);
        JetType type = typeInfo.getType();
        DataFlowInfo dataFlowInfo;
        if (type != null && KotlinBuiltIns.getInstance().isNothing(type)) {
            dataFlowInfo = otherInfo;
        } else {
            dataFlowInfo = typeInfo.getDataFlowInfo().or(otherInfo);
        }
        JetType typeForIfExpression = DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getUnitType(), ifExpression, context);
        return DataFlowUtils.checkImplicitCast(typeForIfExpression, ifExpression, context, isStatement, dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitWhileExpression(@NotNull JetWhileExpression expression, ExpressionTypingContext context) {
        return visitWhileExpression(expression, context, false);
    }

    public JetTypeInfo visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(
                INDEPENDENT);
        JetExpression condition = expression.getCondition();
        DataFlowInfo dataFlowInfo = checkCondition(context.scope, condition, context);

        JetExpression body = expression.getBody();
        if (body != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context, "Scope extended in while's condition");
            DataFlowInfo conditionInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, context).and(dataFlowInfo);
            components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                    scopeToExtend, Collections.singletonList(body),
                    CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(conditionInfo), context.trace);
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
            public Void visitBreakExpression(@NotNull JetBreakExpression breakExpression, List<JetLoopExpression> outerLoops) {
                JetSimpleNameExpression targetLabel = breakExpression.getTargetLabel();
                PsiElement element = targetLabel != null ? context.trace.get(LABEL_TARGET, targetLabel) : null;
                if (element == loopExpression || (targetLabel == null && outerLoops.get(outerLoops.size() - 1) == loopExpression)) {
                    result[0] = true;
                }
                return null;
            }

            @Override
            public Void visitContinueExpression(@NotNull JetContinueExpression expression, List<JetLoopExpression> outerLoops) {
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
            public Void visitLoopExpression(@NotNull JetLoopExpression loopExpression, List<JetLoopExpression> outerLoops) {
                List<JetLoopExpression> newOuterLoops = Lists.newArrayList(outerLoops);
                newOuterLoops.add(loopExpression);
                return super.visitLoopExpression(loopExpression, newOuterLoops);
            }
        }, Lists.newArrayList(loopExpression));

        return result[0];
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(@NotNull JetDoWhileExpression expression, ExpressionTypingContext context) {
        return visitDoWhileExpression(expression, context, false);
    }
    public JetTypeInfo visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        JetExpression body = expression.getBody();
        JetScope conditionScope = context.scope;
        if (body instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression function = (JetFunctionLiteralExpression) body;
            JetFunctionLiteral functionLiteral = function.getFunctionLiteral();
            if (!functionLiteral.hasParameterSpecification()) {
                WritableScope writableScope = newWritableScopeImpl(context, "do..while body scope");
                conditionScope = writableScope;
                components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(writableScope, functionLiteral.getBodyExpression().getStatements(), CoercionStrategy.NO_COERCION, context, context.trace);
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
            components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(writableScope, block, CoercionStrategy.NO_COERCION, context, context.trace);
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
    public JetTypeInfo visitForExpression(@NotNull JetForExpression expression, ExpressionTypingContext context) {
        return visitForExpression(expression, context, false);
    }

    public JetTypeInfo visitForExpression(JetForExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        JetExpression loopRange = expression.getLoopRange();
        JetType expectedParameterType = null;
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (loopRange != null) {
            ExpressionReceiver loopRangeReceiver = getExpressionReceiver(facade, loopRange, context.replaceScope(context.scope));
            dataFlowInfo = facade.getTypeInfo(loopRange, context).getDataFlowInfo();
            if (loopRangeReceiver != null) {
                expectedParameterType = components.forLoopConventionsChecker.checkIterableConvention(loopRangeReceiver, context);
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
                components.expressionTypingUtils.defineLocalVariablesFromMultiDeclaration(loopScope, multiParameter, iteratorNextAsReceiver,
                                                                                          loopRange, context);
            }
        }

        JetExpression body = expression.getBody();
        if (body != null) {
            components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(loopScope, Collections.singletonList(body),
                    CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(dataFlowInfo), context.trace);
        }

        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getUnitType(), expression, contextWithExpectedType, dataFlowInfo);
    }

    private VariableDescriptor createLoopParameterDescriptor(
            JetParameter loopParameter,
            JetType expectedParameterType,
            ExpressionTypingContext context
    ) {
        DescriptorResolver.checkParameterHasNoValOrVar(context.trace, loopParameter, VAL_OR_VAR_ON_LOOP_PARAMETER);

        JetTypeReference typeReference = loopParameter.getTypeReference();
        VariableDescriptor variableDescriptor;
        if (typeReference != null) {
            variableDescriptor = components.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(context.scope, loopParameter, context.trace);
            JetType actualParameterType = variableDescriptor.getType();
            if (expectedParameterType != null &&
                    !JetTypeChecker.DEFAULT.isSubtypeOf(expectedParameterType, actualParameterType)) {
                context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType));
            }
        }
        else {
            if (expectedParameterType == null) {
                expectedParameterType = ErrorUtils.createErrorType("Error");
            }
            variableDescriptor = components.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(loopParameter, expectedParameterType, context.trace, context.scope);
        }

        {
            // http://youtrack.jetbrains.net/issue/KT-527

            VariableDescriptor olderVariable = context.scope.getLocalVariable(variableDescriptor.getName());
            if (olderVariable != null && isLocal(context.scope.getContainingDeclaration(), olderVariable)) {
                PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
                context.trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().asString()));
            }
        }
        return variableDescriptor;
    }

    @Override
    public JetTypeInfo visitTryExpression(@NotNull JetTryExpression expression, ExpressionTypingContext typingContext) {
        ExpressionTypingContext context = typingContext.replaceContextDependency(INDEPENDENT);
        JetExpression tryBlock = expression.getTryBlock();
        List<JetCatchClause> catchClauses = expression.getCatchClauses();
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        List<JetType> types = new ArrayList<JetType>();
        for (JetCatchClause catchClause : catchClauses) {
            JetParameter catchParameter = catchClause.getCatchParameter();
            JetExpression catchBody = catchClause.getCatchBody();
            if (catchParameter != null) {
                DescriptorResolver.checkParameterHasNoValOrVar(context.trace, catchParameter, VAL_OR_VAR_ON_CATCH_PARAMETER);
                DescriptorResolver.checkParameterHasNoModifier(context.trace, catchParameter);

                VariableDescriptor variableDescriptor = components.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(
                        context.scope, catchParameter, context.trace);
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
                                              context.replaceExpectedType(NO_EXPECTED_TYPE)).getDataFlowInfo();
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
    public JetTypeInfo visitThrowExpression(@NotNull JetThrowExpression expression, ExpressionTypingContext context) {
        JetExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            JetType throwableType = KotlinBuiltIns.getInstance().getThrowable().getDefaultType();
            facade.getTypeInfo(thrownExpression, context
                    .replaceExpectedType(throwableType).replaceScope(context.scope).replaceContextDependency(INDEPENDENT));
        }
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getNothingType(), expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitReturnExpression(@NotNull JetReturnExpression expression, ExpressionTypingContext context) {
        JetElement labelTargetElement = LabelResolver.INSTANCE.resolveControlLabel(expression, context);

        JetExpression returnedExpression = expression.getReturnedExpression();

        JetType expectedType = NO_EXPECTED_TYPE;
        JetType resultType = KotlinBuiltIns.getInstance().getNothingType();
        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);

        if (parentDeclaration instanceof JetParameter) {
            // In a default value for parameter
            context.trace.report(RETURN_NOT_ALLOWED.on(expression));
        }

        if (expression.getTargetLabel() == null) {
            assert parentDeclaration != null;
            DeclarationDescriptor declarationDescriptor = context.trace.get(DECLARATION_TO_DESCRIPTOR, parentDeclaration);
            Pair<FunctionDescriptor, PsiElement> containingFunInfo =
                    BindingContextUtils.getContainingFunctionSkipFunctionLiterals(declarationDescriptor, false);
            FunctionDescriptor containingFunctionDescriptor = containingFunInfo.getFirst();

            if (containingFunctionDescriptor != null) {
                if (!InlineDescriptorUtils.checkNonLocalReturnUsage(containingFunctionDescriptor, expression, context.trace) ||
                    containingFunctionDescriptor instanceof ConstructorDescriptor) {
                    // Unqualified, in a function literal
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                    resultType = ErrorUtils.createErrorType(RETURN_NOT_ALLOWED_MESSAGE);
                }

                expectedType = getFunctionExpectedReturnType(containingFunctionDescriptor, (JetElement) containingFunInfo.getSecond());
            }
            else {
                // Outside a function
                context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                resultType = ErrorUtils.createErrorType(RETURN_NOT_ALLOWED_MESSAGE);
            }
        }
        else if (labelTargetElement != null) {
            SimpleFunctionDescriptor functionDescriptor = context.trace.get(FUNCTION, labelTargetElement);
            if (functionDescriptor != null) {
                expectedType = getFunctionExpectedReturnType(functionDescriptor, labelTargetElement);
                if (!InlineDescriptorUtils.checkNonLocalReturnUsage(functionDescriptor, expression, context.trace)) {
                    // Qualified, non-local
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                    resultType = ErrorUtils.createErrorType(RETURN_NOT_ALLOWED_MESSAGE);
                }
                else if (expectedType == NO_EXPECTED_TYPE) {
                    // expectedType is NO_EXPECTED_TYPE iff the return type of the corresponding function descriptor is not computed yet
                    // our temporary policy is to prohibit returns in this case. It mostly applies to local returns in lambdas
                    context.trace.report(RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED.on(expression));
                }
            }
            else {
                context.trace.report(NOT_A_RETURN_LABEL.on(expression, expression.getLabelName()));
            }
        }
        if (returnedExpression != null) {
            facade.getTypeInfo(returnedExpression, context.replaceExpectedType(expectedType).replaceScope(context.scope)
                    .replaceContextDependency(INDEPENDENT));
        }
        else {
            if (expectedType != null && !noExpectedType(expectedType) && !KotlinBuiltIns.getInstance().isUnit(expectedType)) {
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, expectedType));
            }
        }
        return DataFlowUtils.checkType(resultType, expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitBreakExpression(@NotNull JetBreakExpression expression, ExpressionTypingContext context) {
        LabelResolver.INSTANCE.resolveControlLabel(expression, context);
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getNothingType(), expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitContinueExpression(@NotNull JetContinueExpression expression, ExpressionTypingContext context) {
        LabelResolver.INSTANCE.resolveControlLabel(expression, context);
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getNothingType(), expression, context, context.dataFlowInfo);
    }

    @NotNull
    private static JetType getFunctionExpectedReturnType(@NotNull FunctionDescriptor descriptor, @NotNull JetElement function) {
        JetType expectedType;
        if (function instanceof JetFunction) {
            if (((JetFunction) function).getReturnTypeRef() != null || ((JetFunction) function).hasBlockBody()) {
                expectedType = descriptor.getReturnType();
            }
            else {
                expectedType = TypeUtils.NO_EXPECTED_TYPE;
            }
        }
        else {
            expectedType = descriptor.getReturnType();
        }
        return expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE;
    }
}
