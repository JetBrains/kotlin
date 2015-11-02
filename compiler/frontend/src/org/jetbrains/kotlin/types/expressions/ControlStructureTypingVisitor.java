/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.types.CommonSupertypes;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.ResolveConstruct;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.*;
import static org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.createCallForSpecialConstruction;
import static org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.createDataFlowInfoForArgumentsForIfCall;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*;

public class ControlStructureTypingVisitor extends ExpressionTypingVisitor {

    public static final String RETURN_NOT_ALLOWED_MESSAGE = "Return not allowed";

    protected ControlStructureTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @NotNull
    private DataFlowInfo checkCondition(@NotNull LexicalScope scope, @Nullable KtExpression condition, ExpressionTypingContext context) {
        if (condition != null) {
            KotlinTypeInfo typeInfo = facade.getTypeInfo(condition, context.replaceScope(scope)
                    .replaceExpectedType(components.builtIns.getBooleanType()).replaceContextDependency(INDEPENDENT));
            KotlinType conditionType = typeInfo.getType();

            if (conditionType != null && !components.builtIns.isBooleanOrSubtype(conditionType)) {
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(condition, conditionType));
            }

            return typeInfo.getDataFlowInfo();
        }
        return context.dataFlowInfo;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public KotlinTypeInfo visitIfExpression(@NotNull KtIfExpression expression, ExpressionTypingContext context) {
        return visitIfExpression(expression, context, false);
    }

    public KotlinTypeInfo visitIfExpression(KtIfExpression ifExpression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
        KtExpression condition = ifExpression.getCondition();
        DataFlowInfo conditionDataFlowInfo = checkCondition(context.scope, condition, context);

        KtExpression elseBranch = ifExpression.getElse();
        KtExpression thenBranch = ifExpression.getThen();

        LexicalWritableScope thenScope = newWritableScopeImpl(context, "Then scope");
        LexicalWritableScope elseScope = newWritableScopeImpl(context, "Else scope");
        DataFlowInfo thenInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context).and(conditionDataFlowInfo);
        DataFlowInfo elseInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context).and(conditionDataFlowInfo);

        if (elseBranch == null) {
            if (thenBranch != null) {
                KotlinTypeInfo result = getTypeInfoWhenOnlyOneBranchIsPresent(
                        thenBranch, thenScope, thenInfo, elseInfo, contextWithExpectedType, ifExpression, isStatement);
                // If jump was possible, take condition check info as the jump info
                return result.getJumpOutPossible()
                       ? result.replaceJumpOutPossible(true).replaceJumpFlowInfo(conditionDataFlowInfo)
                       : result;
            }
            return TypeInfoFactoryKt.createTypeInfo(components.dataFlowAnalyzer.checkImplicitCast(
                                                                 components.builtIns.getUnitType(), ifExpression,
                                                                 contextWithExpectedType, isStatement
                                                         ),
                                                    thenInfo.or(elseInfo)
            );
        }
        if (thenBranch == null) {
            return getTypeInfoWhenOnlyOneBranchIsPresent(
                    elseBranch, elseScope, elseInfo, thenInfo, contextWithExpectedType, ifExpression, isStatement);
        }
        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(ifExpression);
        KtBlockExpression thenBlock = psiFactory.wrapInABlockWrapper(thenBranch);
        KtBlockExpression elseBlock = psiFactory.wrapInABlockWrapper(elseBranch);
        Call callForIf = createCallForSpecialConstruction(ifExpression, ifExpression, Lists.newArrayList(thenBlock, elseBlock));
        MutableDataFlowInfoForArguments dataFlowInfoForArguments =
                    createDataFlowInfoForArgumentsForIfCall(callForIf, thenInfo, elseInfo);
        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                callForIf, ResolveConstruct.IF, Lists.newArrayList("thenBranch", "elseBranch"),
                Lists.newArrayList(false, false),
                contextWithExpectedType, dataFlowInfoForArguments);

        BindingContext bindingContext = context.trace.getBindingContext();
        KotlinTypeInfo thenTypeInfo = BindingContextUtils.getRecordedTypeInfo(thenBranch, bindingContext);
        KotlinTypeInfo elseTypeInfo = BindingContextUtils.getRecordedTypeInfo(elseBranch, bindingContext);
        assert thenTypeInfo != null : "'Then' branch of if expression  was not processed: " + ifExpression;
        assert elseTypeInfo != null : "'Else' branch of if expression  was not processed: " + ifExpression;
        boolean loopBreakContinuePossible = thenTypeInfo.getJumpOutPossible() || elseTypeInfo.getJumpOutPossible();

        KotlinType thenType = thenTypeInfo.getType();
        KotlinType elseType = elseTypeInfo.getType();
        DataFlowInfo thenDataFlowInfo = thenTypeInfo.getDataFlowInfo();
        DataFlowInfo elseDataFlowInfo = elseTypeInfo.getDataFlowInfo();

        boolean jumpInThen = thenType != null && KotlinBuiltIns.isNothing(thenType);
        boolean jumpInElse = elseType != null && KotlinBuiltIns.isNothing(elseType);

        DataFlowInfo resultDataFlowInfo;
        if (thenType == null && elseType == null) {
            resultDataFlowInfo = thenDataFlowInfo.or(elseDataFlowInfo);
        }
        else if (thenType == null || (jumpInThen && !jumpInElse)) {
            resultDataFlowInfo = elseDataFlowInfo;
        }
        else if (elseType == null || (jumpInElse && !jumpInThen)) {
            resultDataFlowInfo = thenDataFlowInfo;
        }
        else {
            resultDataFlowInfo = thenDataFlowInfo.or(elseDataFlowInfo);
        }

        KotlinType resultType = resolvedCall.getResultingDescriptor().getReturnType();
        // If break or continue was possible, take condition check info as the jump info
        return TypeInfoFactoryKt
                .createTypeInfo(components.dataFlowAnalyzer.checkImplicitCast(resultType, ifExpression, contextWithExpectedType, isStatement),
                                resultDataFlowInfo, loopBreakContinuePossible, conditionDataFlowInfo);
    }

    @NotNull
    private KotlinTypeInfo getTypeInfoWhenOnlyOneBranchIsPresent(
            @NotNull KtExpression presentBranch,
            @NotNull LexicalWritableScope presentScope,
            @NotNull DataFlowInfo presentInfo,
            @NotNull DataFlowInfo otherInfo,
            @NotNull ExpressionTypingContext context,
            @NotNull KtIfExpression ifExpression,
            boolean isStatement
    ) {
        ExpressionTypingContext newContext = context.replaceDataFlowInfo(presentInfo).replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(INDEPENDENT);
        KotlinTypeInfo typeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                presentScope, Collections.singletonList(presentBranch), CoercionStrategy.NO_COERCION, newContext);
        KotlinType type = typeInfo.getType();
        DataFlowInfo dataFlowInfo;
        if (type != null && KotlinBuiltIns.isNothing(type)) {
            dataFlowInfo = otherInfo;
        } else {
            dataFlowInfo = typeInfo.getDataFlowInfo().or(otherInfo);
        }
        return components.dataFlowAnalyzer.checkImplicitCast(
                components.dataFlowAnalyzer.checkType(
                        typeInfo.replaceType(components.builtIns.getUnitType()),
                        ifExpression,
                        context
                ),
                ifExpression,
                context,
                isStatement
        ).replaceDataFlowInfo(dataFlowInfo);
    }

    @Override
    public KotlinTypeInfo visitWhileExpression(@NotNull KtWhileExpression expression, ExpressionTypingContext context) {
        return visitWhileExpression(expression, context, false);
    }

    public KotlinTypeInfo visitWhileExpression(KtWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(
                INDEPENDENT);
        // Preliminary analysis
        PreliminaryLoopVisitor loopVisitor = PreliminaryLoopVisitor.visitLoop(expression);
        context = context.replaceDataFlowInfo(
                loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo)
        );

        KtExpression condition = expression.getCondition();
        // Extract data flow info from condition itself without taking value into account
        DataFlowInfo dataFlowInfo = checkCondition(context.scope, condition, context);

        KtExpression body = expression.getBody();
        KotlinTypeInfo bodyTypeInfo;
        DataFlowInfo conditionInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context).and(dataFlowInfo);
        if (body != null) {
            LexicalWritableScope scopeToExtend = newWritableScopeImpl(context, "Scope extended in while's condition");
            bodyTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                    scopeToExtend, Collections.singletonList(body),
                    CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(conditionInfo));
        }
        else {
            bodyTypeInfo = TypeInfoFactoryKt.noTypeInfo(conditionInfo);
        }

        // Condition is false at this point only if there is no jumps outside
        if (!containsJumpOutOfLoop(expression, context)) {
            dataFlowInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context).and(dataFlowInfo);
        }

        // Special case: while (true)
        // In this case we must record data flow information at the nearest break / continue and
        // .and it with entrance data flow information, because while body until break is executed at least once in this case
        // See KT-6284
        if (body != null && KtPsiUtil.isTrueConstant(condition)) {
            // We should take data flow info from the first jump point,
            // but without affecting changing variables
            dataFlowInfo = dataFlowInfo.and(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(bodyTypeInfo.getJumpFlowInfo()));
        }
        return components.dataFlowAnalyzer
                .checkType(bodyTypeInfo.replaceType(components.builtIns.getUnitType()), expression, contextWithExpectedType)
                .replaceDataFlowInfo(dataFlowInfo);
    }

    private boolean containsJumpOutOfLoop(final KtLoopExpression loopExpression, final ExpressionTypingContext context) {
        final boolean[] result = new boolean[1];
        result[0] = false;
        //todo breaks in inline function literals
        loopExpression.accept(new KtTreeVisitor<List<KtLoopExpression>>() {
            @Override
            public Void visitBreakExpression(@NotNull KtBreakExpression breakExpression, List<KtLoopExpression> outerLoops) {
                KtSimpleNameExpression targetLabel = breakExpression.getTargetLabel();
                PsiElement element = targetLabel != null ? context.trace.get(LABEL_TARGET, targetLabel) : null;
                if (element == loopExpression || (targetLabel == null && outerLoops.get(outerLoops.size() - 1) == loopExpression)) {
                    result[0] = true;
                }
                return null;
            }

            @Override
            public Void visitContinueExpression(@NotNull KtContinueExpression expression, List<KtLoopExpression> outerLoops) {
                // continue@someOuterLoop is also considered as break
                KtSimpleNameExpression targetLabel = expression.getTargetLabel();
                if (targetLabel != null) {
                    PsiElement element = context.trace.get(LABEL_TARGET, targetLabel);
                    if (element instanceof KtLoopExpression && !outerLoops.contains(element)) {
                        result[0] = true;
                    }
                }
                return null;
            }

            @Override
            public Void visitLoopExpression(@NotNull KtLoopExpression loopExpression, List<KtLoopExpression> outerLoops) {
                List<KtLoopExpression> newOuterLoops = Lists.newArrayList(outerLoops);
                newOuterLoops.add(loopExpression);
                return super.visitLoopExpression(loopExpression, newOuterLoops);
            }
        }, Lists.newArrayList(loopExpression));

        return result[0];
    }

    @Override
    public KotlinTypeInfo visitDoWhileExpression(@NotNull KtDoWhileExpression expression, ExpressionTypingContext context) {
        return visitDoWhileExpression(expression, context, false);
    }

    public KotlinTypeInfo visitDoWhileExpression(KtDoWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        KtExpression body = expression.getBody();
        LexicalScope conditionScope = context.scope;
        // Preliminary analysis
        PreliminaryLoopVisitor loopVisitor = PreliminaryLoopVisitor.visitLoop(expression);
        context = context.replaceDataFlowInfo(
                loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo)
        );
        // Here we must record data flow information at the end of the body (or at the first jump, to be precise) and
        // .and it with entrance data flow information, because do-while body is executed at least once
        // See KT-6283
        KotlinTypeInfo bodyTypeInfo;
        if (body instanceof KtFunctionLiteralExpression) {
            // As a matter of fact, function literal is always unused at this point
            bodyTypeInfo = facade.getTypeInfo(body, context.replaceScope(context.scope));
        }
        else if (body != null) {
            LexicalWritableScope writableScope = newWritableScopeImpl(context, "do..while body scope");
            conditionScope = writableScope;
            List<KtExpression> block;
            if (body instanceof KtBlockExpression) {
                block = ((KtBlockExpression)body).getStatements();
            }
            else {
                block = Collections.singletonList(body);
            }
            bodyTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                    writableScope, block, CoercionStrategy.NO_COERCION, context);
        }
        else {
            bodyTypeInfo = TypeInfoFactoryKt.noTypeInfo(context);
        }
        KtExpression condition = expression.getCondition();
        DataFlowInfo conditionDataFlowInfo = checkCondition(conditionScope, condition, context);
        DataFlowInfo dataFlowInfo;
        // Without jumps out, condition is entered and false, with jumps out, we know nothing about it
        if (!containsJumpOutOfLoop(expression, context)) {
            dataFlowInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context).and(conditionDataFlowInfo);
        }
        else {
            dataFlowInfo = context.dataFlowInfo;
        }
        // Here we must record data flow information at the end of the body (or at the first jump, to be precise) and
        // .and it with entrance data flow information, because do-while body is executed at least once
        // See KT-6283
        // NB: it's really important to do it for non-empty body which is not a function literal
        // If it's a function literal, it appears always unused so it's no matter what we do at this point
        if (body != null) {
            // We should take data flow info from the first jump point,
            // but without affecting changing variables
            dataFlowInfo = dataFlowInfo.and(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(bodyTypeInfo.getJumpFlowInfo()));
        }
        return components.dataFlowAnalyzer
                .checkType(bodyTypeInfo.replaceType(components.builtIns.getUnitType()), expression, contextWithExpectedType)
                .replaceDataFlowInfo(dataFlowInfo);
    }

    @Override
    public KotlinTypeInfo visitForExpression(@NotNull KtForExpression expression, ExpressionTypingContext context) {
        return visitForExpression(expression, context, false);
    }

    public KotlinTypeInfo visitForExpression(KtForExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        // Preliminary analysis
        PreliminaryLoopVisitor loopVisitor = PreliminaryLoopVisitor.visitLoop(expression);
        context = context.replaceDataFlowInfo(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo));

        KtExpression loopRange = expression.getLoopRange();
        KotlinType expectedParameterType = null;
        KotlinTypeInfo loopRangeInfo;
        if (loopRange != null) {
            ExpressionReceiver loopRangeReceiver = getExpressionReceiver(facade, loopRange, context.replaceScope(context.scope));
            loopRangeInfo = facade.getTypeInfo(loopRange, context);
            if (loopRangeReceiver != null) {
                expectedParameterType = components.forLoopConventionsChecker.checkIterableConvention(loopRangeReceiver, context);
            }
        }
        else {
            loopRangeInfo = TypeInfoFactoryKt.noTypeInfo(context);
        }

        LexicalWritableScope loopScope = newWritableScopeImpl(context, "Scope with for-loop index");

        KtParameter loopParameter = expression.getLoopParameter();
        if (loopParameter != null) {
            VariableDescriptor variableDescriptor = createLoopParameterDescriptor(loopParameter, expectedParameterType, context);
            components.modifiersChecker.withTrace(context.trace).checkModifiersForLocalDeclaration(loopParameter, variableDescriptor);
            components.identifierChecker.checkDeclaration(loopParameter, context.trace);

            loopScope.addVariableDescriptor(variableDescriptor);
        }
        else {
            KtMultiDeclaration multiParameter = expression.getMultiParameter();
            if (multiParameter != null && loopRange != null) {
                KotlinType elementType = expectedParameterType == null ? ErrorUtils.createErrorType("Loop range has no type") : expectedParameterType;
                TransientReceiver iteratorNextAsReceiver = new TransientReceiver(elementType);
                components.annotationResolver.resolveAnnotationsWithArguments(loopScope, multiParameter.getModifierList(), context.trace);
                components.multiDeclarationResolver.defineLocalVariablesFromMultiDeclaration(
                        loopScope, multiParameter, iteratorNextAsReceiver, loopRange, context
                );
                components.modifiersChecker.withTrace(context.trace).checkModifiersForMultiDeclaration(multiParameter);
                components.modifiersChecker.withTrace(context.trace).checkParameterHasNoValOrVar(multiParameter, VAL_OR_VAR_ON_LOOP_MULTI_PARAMETER);
                components.identifierChecker.checkDeclaration(multiParameter, context.trace);
            }
        }

        KtExpression body = expression.getBody();
        KotlinTypeInfo bodyTypeInfo;
        if (body != null) {
            bodyTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(loopScope, Collections.singletonList(body),
                    CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(loopRangeInfo.getDataFlowInfo()));
        }
        else {
            bodyTypeInfo = loopRangeInfo;
        }

        return components.dataFlowAnalyzer
                .checkType(bodyTypeInfo.replaceType(components.builtIns.getUnitType()), expression, contextWithExpectedType)
                .replaceDataFlowInfo(loopRangeInfo.getDataFlowInfo());
    }

    private VariableDescriptor createLoopParameterDescriptor(
            KtParameter loopParameter,
            KotlinType expectedParameterType,
            ExpressionTypingContext context
    ) {
        components.modifiersChecker.withTrace(context.trace).checkParameterHasNoValOrVar(loopParameter, VAL_OR_VAR_ON_LOOP_PARAMETER);

        KtTypeReference typeReference = loopParameter.getTypeReference();
        VariableDescriptor variableDescriptor;
        if (typeReference != null) {
            variableDescriptor = components.descriptorResolver.
                    resolveLocalVariableDescriptor(context.scope, loopParameter, context.trace);
            KotlinType actualParameterType = variableDescriptor.getType();
            if (expectedParameterType != null &&
                    !KotlinTypeChecker.DEFAULT.isSubtypeOf(expectedParameterType, actualParameterType)) {
                context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType));
            }
        }
        else {
            if (expectedParameterType == null) {
                expectedParameterType = ErrorUtils.createErrorType("Error");
            }
            variableDescriptor = components.descriptorResolver.
                    resolveLocalVariableDescriptor(loopParameter, expectedParameterType, context.trace, context.scope);
        }

        {
            // http://youtrack.jetbrains.net/issue/KT-527

            VariableDescriptor olderVariable = ScopeUtilsKt.findLocalVariable(context.scope, variableDescriptor.getName());
            if (olderVariable != null && isLocal(context.scope.getOwnerDescriptor(), olderVariable)) {
                PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
                context.trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().asString()));
            }
        }
        return variableDescriptor;
    }

    @Override
    public KotlinTypeInfo visitTryExpression(@NotNull KtTryExpression expression, ExpressionTypingContext typingContext) {
        ExpressionTypingContext context = typingContext.replaceContextDependency(INDEPENDENT);
        KtExpression tryBlock = expression.getTryBlock();
        List<KtCatchClause> catchClauses = expression.getCatchClauses();
        KtFinallySection finallyBlock = expression.getFinallyBlock();
        List<KotlinType> types = new ArrayList<KotlinType>();
        for (KtCatchClause catchClause : catchClauses) {
            KtParameter catchParameter = catchClause.getCatchParameter();
            KtExpression catchBody = catchClause.getCatchBody();
            if (catchParameter != null) {
                components.identifierChecker.checkDeclaration(catchParameter, context.trace);
                ModifiersChecker.ModifiersCheckingProcedure modifiersChecking = components.modifiersChecker.withTrace(context.trace);
                modifiersChecking.checkParameterHasNoValOrVar(catchParameter, VAL_OR_VAR_ON_CATCH_PARAMETER);
                ModifierCheckerCore.INSTANCE$.check(catchParameter, context.trace, null);

                VariableDescriptor variableDescriptor = components.descriptorResolver.resolveLocalVariableDescriptor(
                        context.scope, catchParameter, context.trace);
                KotlinType throwableType = components.builtIns.getThrowable().getDefaultType();
                components.dataFlowAnalyzer.checkType(variableDescriptor.getType(), catchParameter, context.replaceExpectedType(throwableType));
                if (catchBody != null) {
                    LexicalWritableScope catchScope = newWritableScopeImpl(context, "Catch scope");
                    catchScope.addVariableDescriptor(variableDescriptor);
                    KotlinType type = facade.getTypeInfo(catchBody, context.replaceScope(catchScope)).getType();
                    if (type != null) {
                        types.add(type);
                    }
                }
            }
        }

        KotlinTypeInfo result = TypeInfoFactoryKt.noTypeInfo(context);
        if (finallyBlock != null) {
            result = facade.getTypeInfo(finallyBlock.getFinalExpression(),
                                        context.replaceExpectedType(NO_EXPECTED_TYPE));
        }

        KotlinType type = facade.getTypeInfo(tryBlock, context).getType();
        if (type != null) {
            types.add(type);
        }
        if (types.isEmpty()) {
            return result.clearType();
        }
        else {
            return result.replaceType(CommonSupertypes.commonSupertype(types));
        }
    }

    @Override
    public KotlinTypeInfo visitThrowExpression(@NotNull KtThrowExpression expression, ExpressionTypingContext context) {
        KtExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            KotlinType throwableType = components.builtIns.getThrowable().getDefaultType();
            facade.getTypeInfo(thrownExpression, context
                    .replaceExpectedType(throwableType).replaceScope(context.scope).replaceContextDependency(INDEPENDENT));
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.getNothingType(), context, expression);
    }

    @Override
    public KotlinTypeInfo visitReturnExpression(@NotNull KtReturnExpression expression, ExpressionTypingContext context) {
        KtElement labelTargetElement = LabelResolver.INSTANCE.resolveControlLabel(expression, context);

        KtExpression returnedExpression = expression.getReturnedExpression();

        KotlinType expectedType = NO_EXPECTED_TYPE;
        KotlinType resultType = components.builtIns.getNothingType();
        KtDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, KtDeclaration.class);

        if (parentDeclaration instanceof KtParameter) {
            // In a default value for parameter
            context.trace.report(RETURN_NOT_ALLOWED.on(expression));
        }

        if (expression.getTargetLabel() == null) {
            while (parentDeclaration instanceof KtMultiDeclaration) {
                //TODO: It's hacking fix for KT-5100: Strange "Return is not allowed here" for multi-declaration initializer with elvis expression
                parentDeclaration = PsiTreeUtil.getParentOfType(parentDeclaration, KtDeclaration.class);
            }

            assert parentDeclaration != null : "Can't find parent declaration for " + expression.getText();
            DeclarationDescriptor declarationDescriptor = context.trace.get(DECLARATION_TO_DESCRIPTOR, parentDeclaration);
            Pair<FunctionDescriptor, PsiElement> containingFunInfo =
                    BindingContextUtils.getContainingFunctionSkipFunctionLiterals(declarationDescriptor, false);
            FunctionDescriptor containingFunctionDescriptor = containingFunInfo.getFirst();

            if (containingFunctionDescriptor != null) {
                if (!InlineUtil.checkNonLocalReturnUsage(containingFunctionDescriptor, expression, context.trace) ||
                    isClassInitializer(containingFunInfo)) {
                    // Unqualified, in a function literal
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                    resultType = ErrorUtils.createErrorType(RETURN_NOT_ALLOWED_MESSAGE);
                }

                expectedType = getFunctionExpectedReturnType(containingFunctionDescriptor, (KtElement) containingFunInfo.getSecond(), context);
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
                expectedType = getFunctionExpectedReturnType(functionDescriptor, labelTargetElement, context);
                if (!InlineUtil.checkNonLocalReturnUsage(functionDescriptor, expression, context.trace)) {
                    // Qualified, non-local
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                    resultType = ErrorUtils.createErrorType(RETURN_NOT_ALLOWED_MESSAGE);
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
            if (expectedType != null &&
                !noExpectedType(expectedType) &&
                !KotlinBuiltIns.isUnit(expectedType) &&
                !isDontCarePlaceholder(expectedType)) // for lambda with implicit return type Unit
            {
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, expectedType));
            }
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(resultType, context, expression);
    }

    private static boolean isClassInitializer(@NotNull Pair<FunctionDescriptor, PsiElement> containingFunInfo) {
        return containingFunInfo.getFirst() instanceof ConstructorDescriptor &&
               !(containingFunInfo.getSecond() instanceof KtSecondaryConstructor);
    }

    @Override
    public KotlinTypeInfo visitBreakExpression(@NotNull KtBreakExpression expression, ExpressionTypingContext context) {
        LabelResolver.INSTANCE.resolveControlLabel(expression, context);
        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.getNothingType(), context, expression).
                replaceJumpOutPossible(true);
    }

    @Override
    public KotlinTypeInfo visitContinueExpression(@NotNull KtContinueExpression expression, ExpressionTypingContext context) {
        LabelResolver.INSTANCE.resolveControlLabel(expression, context);
        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.getNothingType(), context, expression).
                replaceJumpOutPossible(true);
    }

    @NotNull
    private static KotlinType getFunctionExpectedReturnType(
            @NotNull FunctionDescriptor descriptor,
            @NotNull KtElement function,
            @NotNull ExpressionTypingContext context
    ) {
        KotlinType expectedType;
        if (function instanceof KtSecondaryConstructor) {
            expectedType = DescriptorUtilsKt.getBuiltIns(descriptor).getUnitType();
        }
        else if (function instanceof KtFunction) {
            KtFunction ktFunction = (KtFunction) function;
            expectedType = context.trace.get(EXPECTED_RETURN_TYPE, ktFunction);

            if ((expectedType == null) && (ktFunction.getTypeReference() != null || ktFunction.hasBlockBody())) {
                expectedType = descriptor.getReturnType();
            }
        }
        else {
            expectedType = descriptor.getReturnType();
        }
        return expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE;
    }
}
