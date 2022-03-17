/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.ModifierCheckerCore;
import org.jetbrains.kotlin.resolve.ModifiersChecker;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.resolve.calls.util.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.tower.KotlinResolutionCallbacksImpl;
import org.jetbrains.kotlin.resolve.calls.tower.LambdaContextInfo;
import org.jetbrains.kotlin.resolve.checkers.TrailingCommaChecker;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.serialization.deserialization.SuspendFunctionTypeUtilKt;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.error.ErrorTypeKind;
import org.jetbrains.kotlin.types.error.ErrorUtils;
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.ResolveConstruct;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.*;
import static org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.*;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingServices.getNewInferenceLambdaInfo;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*;

public class ControlStructureTypingVisitor extends ExpressionTypingVisitor {
    protected ControlStructureTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @NotNull
    private DataFlowInfo checkCondition(@Nullable KtExpression condition, @NotNull ExpressionTypingContext context) {
        if (condition != null) {
            ExpressionTypingContext conditionContext =
                    context.replaceExpectedType(components.builtIns.getBooleanType()).replaceContextDependency(INDEPENDENT);
            KotlinTypeInfo typeInfo = facade.getTypeInfo(condition, conditionContext);

            return components.dataFlowAnalyzer.checkType(typeInfo, condition, conditionContext).getDataFlowInfo();
        }
        return context.dataFlowInfo;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public KotlinTypeInfo visitIfExpression(@NotNull KtIfExpression ifExpression, ExpressionTypingContext context) {
        KtExpression condition = ifExpression.getCondition();
        DataFlowInfo conditionDataFlowInfo = checkCondition(condition, context);
        boolean loopBreakContinuePossibleInCondition = condition != null && containsJumpOutOfLoop(condition, context);

        KtExpression elseBranch = ifExpression.getElse();
        KtExpression thenBranch = ifExpression.getThen();

        LexicalWritableScope thenScope = newWritableScopeImpl(context, LexicalScopeKind.THEN, components.overloadChecker);
        LexicalWritableScope elseScope = newWritableScopeImpl(context, LexicalScopeKind.ELSE, components.overloadChecker);
        DataFlowInfo thenInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context).and(conditionDataFlowInfo);
        DataFlowInfo elseInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context).and(conditionDataFlowInfo);

        if (elseBranch == null) {
            if (thenBranch != null) {
                KotlinTypeInfo result = getTypeInfoWhenOnlyOneBranchIsPresent(
                        thenBranch, thenScope, thenInfo, elseInfo, context, ifExpression);
                // If jump was possible, take condition check info as the jump info
                return result.getJumpOutPossible()
                       ? result.replaceJumpOutPossible(true).replaceJumpFlowInfo(conditionDataFlowInfo)
                       : result;
            }
            return TypeInfoFactoryKt.createTypeInfo(components.builtIns.getUnitType(), thenInfo.or(elseInfo));
        }
        if (thenBranch == null) {
            return getTypeInfoWhenOnlyOneBranchIsPresent(
                    elseBranch, elseScope, elseInfo, thenInfo, context, ifExpression);
        }
        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(ifExpression, false);
        KtBlockExpression thenBlock = psiFactory.wrapInABlockWrapper(thenBranch);
        KtBlockExpression elseBlock = psiFactory.wrapInABlockWrapper(elseBranch);
        Call callForIf = createCallForSpecialConstruction(ifExpression, ifExpression, Lists.newArrayList(thenBlock, elseBlock));
        MutableDataFlowInfoForArguments dataFlowInfoForArguments =
                    createDataFlowInfoForArgumentsForIfCall(callForIf, conditionDataFlowInfo, thenInfo, elseInfo);
        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                callForIf, ResolveConstruct.IF, Lists.newArrayList("thenBranch", "elseBranch"),
                Lists.newArrayList(false, false),
                context, dataFlowInfoForArguments);

        return processIfBranches(
                ifExpression, context, conditionDataFlowInfo,
                loopBreakContinuePossibleInCondition, elseBranch, thenBranch, resolvedCall);
    }

    @NotNull
    private KotlinTypeInfo processIfBranches(
            KtIfExpression ifExpression,
            ExpressionTypingContext context,
            DataFlowInfo conditionDataFlowInfo,
            boolean loopBreakContinuePossibleInCondition,
            KtExpression elseBranch,
            KtExpression thenBranch,
            ResolvedCall<FunctionDescriptor> resolvedCall
    ) {
        BindingContext bindingContext = context.trace.getBindingContext();
        KotlinTypeInfo thenTypeInfo = BindingContextUtils.getRecordedTypeInfo(thenBranch, bindingContext);
        KotlinTypeInfo elseTypeInfo = BindingContextUtils.getRecordedTypeInfo(elseBranch, bindingContext);

        boolean isThenPostponed = ArgumentTypeResolver.isFunctionLiteralOrCallableReference(thenBranch, context);
        boolean isElsePostponed = ArgumentTypeResolver.isFunctionLiteralOrCallableReference(thenBranch, context);

        assert thenTypeInfo != null || elseTypeInfo != null ||
               isThenPostponed || isElsePostponed : "Both branches of if expression were not processed: " + ifExpression.getText();

        if (thenTypeInfo == null && elseTypeInfo == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }

        KotlinType resultType = resolvedCall.getResultingDescriptor().getReturnType();
        boolean loopBreakContinuePossible = loopBreakContinuePossibleInCondition;
        DataFlowInfo resultDataFlowInfo;

        if (elseTypeInfo == null) {
            loopBreakContinuePossible |= thenTypeInfo.getJumpOutPossible();
            resultDataFlowInfo = thenTypeInfo.getDataFlowInfo();
        }
        else if (thenTypeInfo == null) {
            loopBreakContinuePossible |= elseTypeInfo.getJumpOutPossible();
            resultDataFlowInfo = elseTypeInfo.getDataFlowInfo();
        }
        else {
            KotlinType thenType = thenTypeInfo.getType();
            KotlinType elseType = elseTypeInfo.getType();
            DataFlowInfo thenDataFlowInfo = thenTypeInfo.getDataFlowInfo();
            DataFlowInfo elseDataFlowInfo = elseTypeInfo.getDataFlowInfo();
            if (resultType != null && thenType != null && elseType != null) {
                DataFlowValue resultValue = components.dataFlowValueFactory.createDataFlowValue(ifExpression, resultType, context);
                DataFlowValue thenValue = components.dataFlowValueFactory.createDataFlowValue(thenBranch, thenType, context);
                thenDataFlowInfo = thenDataFlowInfo.assign(resultValue, thenValue, components.languageVersionSettings);
                DataFlowValue elseValue = components.dataFlowValueFactory.createDataFlowValue(elseBranch, elseType, context);
                elseDataFlowInfo = elseDataFlowInfo.assign(resultValue, elseValue, components.languageVersionSettings);
            }

            loopBreakContinuePossible |= thenTypeInfo.getJumpOutPossible() || elseTypeInfo.getJumpOutPossible();

            boolean jumpInThen = thenType != null && KotlinBuiltIns.isNothing(thenType);
            boolean jumpInElse = elseType != null && KotlinBuiltIns.isNothing(elseType);

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
            if (thenType == null && jumpInElse ||
                elseType == null && jumpInThen) {
                return TypeInfoFactoryKt.noTypeInfo(resultDataFlowInfo);
            }
        }

        // If break or continue was possible, take condition check info as the jump info
        return TypeInfoFactoryKt.createTypeInfo(
                components.dataFlowAnalyzer.checkType(resultType, ifExpression, context),
                resultDataFlowInfo, loopBreakContinuePossible,
                loopBreakContinuePossibleInCondition ? context.dataFlowInfo : conditionDataFlowInfo);
    }

    @NotNull
    private KotlinTypeInfo getTypeInfoWhenOnlyOneBranchIsPresent(
            @NotNull KtExpression presentBranch,
            @NotNull LexicalWritableScope presentScope,
            @NotNull DataFlowInfo presentInfo,
            @NotNull DataFlowInfo otherInfo,
            @NotNull ExpressionTypingContext context,
            @NotNull KtIfExpression ifExpression
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
        return components.dataFlowAnalyzer.checkType(
                typeInfo.replaceType(components.builtIns.getUnitType()),
                ifExpression,
                context
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
                loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo, components.languageVersionSettings)
        );

        KtExpression condition = expression.getCondition();
        // Extract data flow info from condition itself without taking value into account
        DataFlowInfo dataFlowInfo = checkCondition(condition, context);

        KtExpression body = expression.getBody();
        KotlinTypeInfo bodyTypeInfo;
        DataFlowInfo conditionInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context).and(dataFlowInfo);
        if (body != null) {
            LexicalWritableScope scopeToExtend = newWritableScopeImpl(context, LexicalScopeKind.WHILE_BODY, components.overloadChecker);
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
            dataFlowInfo = dataFlowInfo.and(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(bodyTypeInfo.getJumpFlowInfo(),
                                                                                                   components.languageVersionSettings));
        }
        return components.dataFlowAnalyzer
                .checkType(bodyTypeInfo.replaceType(components.builtIns.getUnitType()), expression, contextWithExpectedType)
                .replaceDataFlowInfo(dataFlowInfo);
    }

    private boolean containsJumpOutOfLoop(@NotNull KtExpression expression, ExpressionTypingContext context) {
        boolean[] result = new boolean[1];
        result[0] = false;
        //todo breaks in inline function literals
        expression.accept(new KtTreeVisitor<List<KtLoopExpression>>() {
            @Override
            public Void visitBreakExpression(@NotNull KtBreakExpression breakExpression, List<KtLoopExpression> outerLoops) {
                KtSimpleNameExpression targetLabel = breakExpression.getTargetLabel();
                PsiElement element = targetLabel != null ? context.trace.get(LABEL_TARGET, targetLabel) : null;
                if (outerLoops.isEmpty() || element == expression ||
                    (targetLabel == null && outerLoops.get(outerLoops.size() - 1) == expression)) {
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
        }, expression instanceof KtLoopExpression ? Lists.newArrayList((KtLoopExpression) expression) : Lists.newArrayList());

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
                loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo, components.languageVersionSettings)
        );
        // Here we must record data flow information at the end of the body (or at the first jump, to be precise) and
        // .and it with entrance data flow information, because do-while body is executed at least once
        // See KT-6283
        KotlinTypeInfo bodyTypeInfo;
        if (body instanceof KtLambdaExpression) {
            // As a matter of fact, function literal is always unused at this point
            bodyTypeInfo = facade.getTypeInfo(body, context);
        }
        else if (body != null) {
            LexicalWritableScope writableScope = newWritableScopeImpl(context, LexicalScopeKind.DO_WHILE_BODY, components.overloadChecker);
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
        DataFlowInfo conditionDataFlowInfo = checkCondition(condition, context.replaceScope(conditionScope));
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
            dataFlowInfo = dataFlowInfo.and(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(bodyTypeInfo.getJumpFlowInfo(),
                                                                                                   components.languageVersionSettings));
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
        context = context.replaceDataFlowInfo(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo,
                                                                                                     components.languageVersionSettings));

        KtExpression loopRange = expression.getLoopRange();
        KotlinType expectedParameterType = null;
        KotlinTypeInfo loopRangeInfo;
        if (loopRange != null) {
            ExpressionReceiver loopRangeReceiver = getExpressionReceiver(facade, loopRange, context);
            loopRangeInfo = facade.getTypeInfo(loopRange, context);
            if (loopRangeReceiver != null) {
                expectedParameterType = components.forLoopConventionsChecker.checkIterableConvention(loopRangeReceiver, context);
            }
        }
        else {
            loopRangeInfo = TypeInfoFactoryKt.noTypeInfo(context);
        }

        LexicalWritableScope loopScope = newWritableScopeImpl(context, LexicalScopeKind.FOR, components.overloadChecker);

        KtParameter loopParameter = expression.getLoopParameter();
        if (loopParameter != null) {
            VariableDescriptor variableDescriptor = createLoopParameterDescriptor(loopParameter, expectedParameterType, context);
            ModifiersChecker.ModifiersCheckingProcedure modifiersCheckingProcedure = components.modifiersChecker.withTrace(context.trace);
            modifiersCheckingProcedure.checkModifiersForLocalDeclaration(loopParameter, variableDescriptor);
            components.identifierChecker.checkDeclaration(loopParameter, context.trace);
            loopScope.addVariableDescriptor(variableDescriptor);
            KtDestructuringDeclaration multiParameter = loopParameter.getDestructuringDeclaration();
            if (multiParameter != null) {
                KotlinType elementType = expectedParameterType == null ? ErrorUtils.createErrorType(ErrorTypeKind.NO_TYPE_FOR_LOOP_RANGE) : expectedParameterType;
                TransientReceiver iteratorNextAsReceiver = new TransientReceiver(elementType);
                components.annotationResolver.resolveAnnotationsWithArguments(loopScope, loopParameter.getModifierList(), context.trace);
                components.destructuringDeclarationResolver.defineLocalVariablesFromDestructuringDeclaration(
                        loopScope, multiParameter, iteratorNextAsReceiver, loopRange, context
                );
                modifiersCheckingProcedure.checkModifiersForDestructuringDeclaration(multiParameter);
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
                expectedParameterType = ErrorUtils.createErrorType(ErrorTypeKind.NO_TYPE_FOR_LOOP_PARAMETER);
            }
            variableDescriptor = components.descriptorResolver.
                    resolveLocalVariableDescriptor(loopParameter, expectedParameterType, context.trace, context.scope);
        }

        checkVariableShadowing(context.scope, context.trace, variableDescriptor);

        return variableDescriptor;
    }

    @Override
    public KotlinTypeInfo visitTryExpression(@NotNull KtTryExpression expression, ExpressionTypingContext typingContext) {
        expression.getCatchClauses().forEach((catchClause) -> {
            KtParameterList parameters = catchClause.getParameterList();
            if (parameters != null && parameters.getStub() == null) {
                TrailingCommaChecker.INSTANCE.check(parameters.getTrailingComma(), typingContext.trace, typingContext.languageVersionSettings);
            }
        });

        if (typingContext.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) {
            return resolveTryExpressionWithNewInference(expression, typingContext);
        }
        ExpressionTypingContext context = typingContext.replaceContextDependency(INDEPENDENT);
        KtExpression tryBlock = expression.getTryBlock();
        List<KtCatchClause> catchClauses = expression.getCatchClauses();
        KtFinallySection finallyBlock = expression.getFinallyBlock();
        List<KotlinType> types = new ArrayList<>();
        boolean nothingInAllCatchBranches = true;
        for (KtCatchClause catchClause : catchClauses) {
            KtParameter catchParameter = catchClause.getCatchParameter();
            KtExpression catchBody = catchClause.getCatchBody();
            boolean nothingInCatchBranch = false;
            if (catchParameter != null) {
                VariableDescriptor variableDescriptor = resolveAndCheckCatchParameter(catchParameter, context);

                if (catchBody != null) {
                    LexicalWritableScope catchScope = newWritableScopeImpl(context, LexicalScopeKind.CATCH, components.overloadChecker);
                    catchScope.addVariableDescriptor(variableDescriptor);
                    KotlinType type = facade.getTypeInfo(catchBody, context.replaceScope(catchScope)).getType();
                    if (type != null) {
                        types.add(type);
                        if (KotlinBuiltIns.isNothing(type)) {
                            nothingInCatchBranch = true;
                        }
                    }
                }
            }
            if (!nothingInCatchBranch) {
                nothingInAllCatchBranches =  false;
            }
        }

        KotlinTypeInfo tryResult = facade.getTypeInfo(tryBlock, context);
        ExpressionTypingContext tryOutputContext = getCleanedContextFromTryWithAssignmentsToVar(expression, nothingInAllCatchBranches, context);

        KotlinTypeInfo result = TypeInfoFactoryKt.noTypeInfo(tryOutputContext);
        if (finallyBlock != null) {
            result = facade.getTypeInfo(finallyBlock.getFinalExpression(), tryOutputContext);
        }
        else if (nothingInAllCatchBranches) {
            result = tryResult;
        }

        KotlinType type = tryResult.getType();
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

    private KotlinTypeInfo resolveTryExpressionWithNewInference(@NotNull KtTryExpression tryExpression, ExpressionTypingContext tryInputContext) {
        // tryInputContext is an ExpressionTypingContext before try/catch expression

        KtBlockExpression tryBlock = tryExpression.getTryBlock();
        List<KtCatchClause> catchClauses = tryExpression.getCatchClauses();
        KtFinallySection finallySection = tryExpression.getFinallyBlock();

        DataFlowInfo dataFlowInfoBeforeTry = tryInputContext.dataFlowInfo;

        /*  tryOutputContext is tryInputContext rom which all dataFlowInfo about vars
         *  assigned inside try block has been cut. E.g.:
         *      var s: String? = ...
         *      var x: String? = null
         *      x = ""  // here we have DFI that x != null and it is tryInputContext
         *      try {
         *         x = null  // here was assignment so we cut info about x from context
         *         // or
         *         x = "42"
         *         requireNotNull(s)
         *      } // here we have tryOutputContext with no info about x
         *        // Notice, that in tryOutputContext we also have no info about s
         *        //   tryOutputContext is just cut tryInputContext
         *      catch (...) { }
         *      ...
         *
         */

        PreliminaryLoopVisitor tryVisitor = PreliminaryLoopVisitor.visitTryBlock(tryExpression);
        ExpressionTypingContext tryOutputContext = tryInputContext.replaceDataFlowInfo(
                tryVisitor.clearDataFlowInfoForAssignedLocalVariables(dataFlowInfoBeforeTry, components.languageVersionSettings)
        );
        DataFlowInfo dataFlowInfoAfterTry = tryOutputContext.dataFlowInfo;

        List<KtExpression> catchBlocks = Lists.newArrayList();
        List<kotlin.Pair<KtExpression, VariableDescriptor>> catchClausesBlocksAndParameters = Lists.newArrayList();

        for (KtCatchClause catchClause : catchClauses) {
            KtParameter catchParameter = catchClause.getCatchParameter();
            KtExpression catchBody = catchClause.getCatchBody();
            if (catchParameter != null) {
                VariableDescriptor variableDescriptor = resolveAndCheckCatchParameter(catchParameter, tryInputContext);
                if (catchBody != null) {
                    catchBlocks.add(catchBody);
                    catchClausesBlocksAndParameters.add(new kotlin.Pair<>(catchBody, variableDescriptor));
                }
            }
        }

        KtBlockExpression finallyBlock = null;
        if (finallySection != null) {
            finallyBlock = finallySection.getFinalExpression();
        }

        List<KtExpression> arguments = Lists.newArrayList(tryBlock);
        arguments.addAll(catchBlocks);

        Call callForTry = createCallForSpecialConstruction(tryExpression, tryExpression, arguments);

        MutableDataFlowInfoForArguments dataFlowInfoForArguments;
        if (components.languageVersionSettings.supportsFeature(LanguageFeature.NewDataFlowForTryExpressions)) {
            dataFlowInfoForArguments = createDataFlowInfoForArgumentsOfTryCall(callForTry, dataFlowInfoBeforeTry, dataFlowInfoAfterTry);
        } else {
            dataFlowInfoForArguments = createDataFlowInfoForArgumentsOfTryCall(callForTry, dataFlowInfoBeforeTry, dataFlowInfoBeforeTry);
        }
        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils
                .resolveTryAsCall(callForTry, catchClausesBlocksAndParameters, tryInputContext, dataFlowInfoForArguments);
        KotlinType resultType = resolvedCall.getResultingDescriptor().getReturnType();

        BindingContext bindingContext = tryInputContext.trace.getBindingContext();

        if (components.languageVersionSettings.supportsFeature(LanguageFeature.NewDataFlowForTryExpressions)) {
            return processTryBranchesWithNewDataFlowAlgorithm(tryExpression, tryBlock, tryOutputContext, dataFlowInfoAfterTry, catchBlocks, finallyBlock, bindingContext, resultType);
        } else {
            return processTryBranches(tryExpression, tryBlock, tryInputContext, catchBlocks, finallyBlock, bindingContext, resultType);
        }
    }

    @NotNull
    private KotlinTypeInfo processTryBranches(
            @NotNull KtTryExpression tryExpression,
            KtBlockExpression tryBlock,
            ExpressionTypingContext context,
            List<KtExpression> catchBlocks,
            KtBlockExpression finallyBlock,
            BindingContext bindingContext,
            KotlinType resultType
    ) {
        KotlinTypeInfo tryInfo = BindingContextUtils.getRecordedTypeInfo(tryBlock, bindingContext);
        DataFlowInfo dataFlowInfoAfterTry;
        if (tryInfo != null) {
            dataFlowInfoAfterTry = tryInfo.getDataFlowInfo();
        }
        else {
            dataFlowInfoAfterTry = DataFlowInfo.Companion.getEMPTY();
        }
        boolean nothingInAllCatchBranches = isCatchBranchesReturnsNothing(catchBlocks, bindingContext);

        // it is not actually correct way (#KT-28370) of computing context, but it's how was in OI
        // Fix of it is breaking change and allowed with NewDataFlowForTryExpressions language feature.
        //   See [processTryBranchesWithNewDataFlowAlgorithm] function
        ExpressionTypingContext tryOutputContext = getCleanedContextFromTryWithAssignmentsToVar(tryExpression, nothingInAllCatchBranches, context)
                .replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(INDEPENDENT);

        KotlinTypeInfo result = TypeInfoFactoryKt.createTypeInfo(resultType, tryOutputContext);

        if (finallyBlock != null) {
            return facade.getTypeInfo(finallyBlock, tryOutputContext).replaceType(resultType);
        } else if (!nothingInAllCatchBranches || tryInfo == null) {
            return result;
        } else {
            return TypeInfoFactoryKt.createTypeInfo(
                    components.dataFlowAnalyzer.checkType(resultType, tryExpression, tryOutputContext),
                    dataFlowInfoAfterTry
            );
        }
    }

    @NotNull
    private KotlinTypeInfo processTryBranchesWithNewDataFlowAlgorithm(
            @NotNull KtTryExpression tryExpression,
            KtBlockExpression tryBlock,
            ExpressionTypingContext tryOutputContext,
            DataFlowInfo dataFlowInfoAfterTry,
            List<KtExpression> catchBlocks,
            KtBlockExpression finallyBlock,
            BindingContext bindingContext,
            KotlinType resultType
    ) {
        /*
         * See [resolveTryExpressionWithNewInference] for the definition of tryOutputContext
         * Here was added some others context and another definitions:
         * - catchOutputContextFromNonNothingBranches is a tryOutputContext without information about variables
         *     assigned in non-Nothing catch branches
         * - catchOutputContextFromAllBranches is tryOutputContext that was cut with assignments from all catch branches
         * - tryInfo is resolved KotlinTypeInfo of try branch, so its dataFlowInfo contains all interesting infos from try block
         *     (e.g. info about s != null from example in function [resolveTryExpressionWithNewInference])
         * - finallyTypeInfo is resolved KotlinTypeInfo of finally branch (it's resolved with assumption that we can came into
         *     finally block from any catch block, even if it returns Nothing)
         * - resultDataFlowInfo is dataFlowInfo that leaves after try/catch/finally (if catch or finally is presented)
         *
         *
         * All analysis passes under strict assumption, that we can fail with exception in any place of try block, so we can't use any
         *     resolved dataFlowInfo from it. But, there is a one case, when we can use it: if there are no catch branches or all catch
         *     branches returns Nothing, so we can reach code after try only if there was no exceptions in try block, so we can use
         *     dataFlowInfo from it (see nothingInAllCatchBranches variable)
         */
        List<Boolean> branchesReturningNothing = whichCatchBranchesReturnNothing(catchBlocks, bindingContext);
        PreliminaryLoopVisitor catchVisitorForNonNothingBranches = PreliminaryLoopVisitor.visitCatchBlocks(tryExpression, mapNot(branchesReturningNothing));
        ExpressionTypingContext catchOutputContextFromNonNothingBranches = tryOutputContext.replaceDataFlowInfo(
                catchVisitorForNonNothingBranches.clearDataFlowInfoForAssignedLocalVariables(dataFlowInfoAfterTry, components.languageVersionSettings)
        );

        KotlinTypeInfo tryInfo = BindingContextUtils.getRecordedTypeInfo(tryBlock, bindingContext);
        boolean nothingInAllCatchBranches = CollectionsKt.all(branchesReturningNothing, it -> it);
        DataFlowInfo nonExceptionalTryCatchesOutputInfo;
        if (tryInfo == null) {
            nonExceptionalTryCatchesOutputInfo = DataFlowInfo.Companion.getEMPTY();
        } else if (nothingInAllCatchBranches) {
            nonExceptionalTryCatchesOutputInfo = tryInfo.getDataFlowInfo();
        } else {
            nonExceptionalTryCatchesOutputInfo = catchOutputContextFromNonNothingBranches.dataFlowInfo;
        }

        DataFlowInfo resultDataFlowInfo;
        if (finallyBlock != null) {
            PreliminaryLoopVisitor catchVisitor = PreliminaryLoopVisitor.visitCatchBlocks(tryExpression);
            ExpressionTypingContext catchOutputContextFromAllBranches = tryOutputContext.replaceDataFlowInfo(
                    catchVisitor.clearDataFlowInfoForAssignedLocalVariables(dataFlowInfoAfterTry, components.languageVersionSettings)
            ).replaceContextDependency(INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE);
            KotlinTypeInfo finallyTypeInfo = facade.getTypeInfo(finallyBlock, catchOutputContextFromAllBranches);
            DataFlowInfo finallyDataFlowInfo = finallyTypeInfo.getDataFlowInfo();
            resultDataFlowInfo = finallyDataFlowInfo.and(nonExceptionalTryCatchesOutputInfo);
        } else {
            resultDataFlowInfo = nonExceptionalTryCatchesOutputInfo;
        }

        return TypeInfoFactoryKt.createTypeInfo(
                components.dataFlowAnalyzer.checkType(resultType, tryExpression, catchOutputContextFromNonNothingBranches),
                resultDataFlowInfo
        );
    }

    private static List<Boolean> mapNot(List<Boolean> list) {
        return CollectionsKt.map(list, it -> !it);
    }

    private static boolean isCatchBranchesReturnsNothing(List<KtExpression> catchBlocks, BindingContext bindingContext) {
        return CollectionsKt.all(whichCatchBranchesReturnNothing(catchBlocks, bindingContext), it -> it);
    }

    private static List<Boolean> whichCatchBranchesReturnNothing(List<KtExpression> catchBlocks, BindingContext bindingContext) {
        return catchBlocks.stream()
                .map(catchBlock -> BindingContextUtils.getRecordedTypeInfo(catchBlock, bindingContext))
                .map(catchTypeInfo -> {
                    if (catchTypeInfo == null) return true;
                    KotlinType catchType = catchTypeInfo.getType();
                    return catchType == null || KotlinBuiltIns.isNothing(catchType);
                })
                .collect(Collectors.toList());
    }

    private VariableDescriptor resolveAndCheckCatchParameter(@NotNull KtParameter catchParameter, ExpressionTypingContext context) {
        checkCatchParameterDeclaration(catchParameter, context);

        VariableDescriptor variableDescriptor = components.descriptorResolver
                .resolveLocalVariableDescriptor(context.scope, catchParameter, context.trace);
        KotlinType catchParameterType = variableDescriptor.getType();
        checkCatchParameterType(catchParameter, catchParameterType, context);
        KotlinType throwableType = components.builtIns.getThrowable().getDefaultType();
        components.dataFlowAnalyzer.checkType(catchParameterType, catchParameter, context.replaceExpectedType(throwableType));
        return variableDescriptor;
    }

    private ExpressionTypingContext getCleanedContextFromTryWithAssignmentsToVar(
            KtTryExpression tryExpression,
            boolean nothingInAllCatchBranches,
            ExpressionTypingContext context
    ) {
        context = context.replaceExpectedType(NO_EXPECTED_TYPE);
        if (!nothingInAllCatchBranches && facade.getComponents().languageVersionSettings.supportsFeature(LanguageFeature.SoundSmartCastsAfterTry)) {
            PreliminaryLoopVisitor tryVisitor = PreliminaryLoopVisitor.visitTryBlock(tryExpression);
            context = context.replaceDataFlowInfo(
                    tryVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo, components.languageVersionSettings)
            );
        }
        return context;
    }

    private static void checkCatchParameterType(KtParameter catchParameter, KotlinType catchParameterType, ExpressionTypingContext context) {
        TypeParameterDescriptor typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(catchParameterType);
        if (typeParameterDescriptor != null) {
            if (typeParameterDescriptor.isReified()) {
                context.trace.report(REIFIED_TYPE_IN_CATCH_CLAUSE.on(catchParameter));
            }
            else {
                context.trace.report(TYPE_PARAMETER_IN_CATCH_CLAUSE.on(catchParameter));
            }
        }
    }

    private void checkCatchParameterDeclaration(KtParameter catchParameter, ExpressionTypingContext context) {
        components.identifierChecker.checkDeclaration(catchParameter, context.trace);
        ModifiersChecker.ModifiersCheckingProcedure modifiersChecking = components.modifiersChecker.withTrace(context.trace);
        modifiersChecking.checkParameterHasNoValOrVar(catchParameter, VAL_OR_VAR_ON_CATCH_PARAMETER);
        ModifierCheckerCore.INSTANCE.check(catchParameter, context.trace, null, components.languageVersionSettings);

        if (catchParameter.hasDefaultValue()) {
            context.trace.report(Errors.CATCH_PARAMETER_WITH_DEFAULT_VALUE.on(catchParameter));
        }
    }

    @Override
    public KotlinTypeInfo visitThrowExpression(@NotNull KtThrowExpression expression, ExpressionTypingContext context) {
        KtExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            KotlinType throwableType = components.builtIns.getThrowable().getDefaultType();
            facade.getTypeInfo(thrownExpression, context.replaceExpectedType(throwableType).replaceContextDependency(INDEPENDENT));
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.getNothingType(), context, expression);
    }

    @Override
    public KotlinTypeInfo visitReturnExpression(@NotNull KtReturnExpression expression, ExpressionTypingContext context) {
        KtElement labelTargetElement = LabelResolver.INSTANCE.resolveControlLabel(expression, context);

        KtExpression returnedExpression = expression.getReturnedExpression();

        KotlinResolutionCallbacksImpl.LambdaInfo newInferenceLambdaInfo = null;

        KotlinType expectedType = NO_EXPECTED_TYPE;
        KotlinType resultType = components.builtIns.getNothingType();
        KtDeclaration parentDeclaration = context.getContextParentOfType(expression, KtDeclaration.class);

        if (parentDeclaration instanceof KtParameter) {
            // In a default value for parameter
            context.trace.report(RETURN_NOT_ALLOWED.on(expression));
        }

        if (expression.getTargetLabel() == null) {
            while (parentDeclaration instanceof KtDestructuringDeclaration) {
                //TODO: It's hacking fix for KT-5100: Strange "Return is not allowed here" for multi-declaration initializer with elvis expression
                parentDeclaration = context.getContextParentOfType(parentDeclaration, KtDeclaration.class);
            }

            // Parent declaration can be null in code fragments or in some bad error expressions
            DeclarationDescriptor declarationDescriptor = context.trace.get(DECLARATION_TO_DESCRIPTOR, parentDeclaration);

            Pair<FunctionDescriptor, PsiElement> containingFunInfo =
                    BindingContextUtils.getContainingFunctionSkipFunctionLiterals(declarationDescriptor, false);
            FunctionDescriptor containingFunctionDescriptor = containingFunInfo.getFirst();

            if (containingFunctionDescriptor != null) {
                if (!InlineUtil.checkNonLocalReturnUsage(containingFunctionDescriptor, expression, context) ||
                    isClassInitializer(containingFunInfo)) {
                    // Unqualified, in a function literal
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                    resultType = ErrorUtils.createErrorType(ErrorTypeKind.RETURN_NOT_ALLOWED);
                }

                expectedType = getFunctionExpectedReturnType(containingFunctionDescriptor, (KtElement) containingFunInfo.getSecond(), context);
                newInferenceLambdaInfo = getNewInferenceLambdaInfo(context, (KtElement) containingFunInfo.getSecond());
            }
            else {
                // Outside a function
                context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                resultType = ErrorUtils.createErrorType(ErrorTypeKind.RETURN_NOT_ALLOWED);
            }
        }
        else if (labelTargetElement != null) {
            SimpleFunctionDescriptor functionDescriptor = context.trace.get(FUNCTION, labelTargetElement);
            if (functionDescriptor != null) {
                expectedType = getFunctionExpectedReturnType(functionDescriptor, labelTargetElement, context);
                newInferenceLambdaInfo = getNewInferenceLambdaInfo(context, labelTargetElement);
                if (!InlineUtil.checkNonLocalReturnUsage(functionDescriptor, expression, context)) {
                    // Qualified, non-local
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                    resultType = ErrorUtils.createErrorType(ErrorTypeKind.RETURN_NOT_ALLOWED);
                }
                else if (labelTargetElement instanceof KtFunctionLiteral
                         && Objects.equals(expression.getLabelName(), "suspend")) {
                    KtExpression callExpression = KtPsiUtil.getParentCallIfPresent((KtFunction) labelTargetElement);
                    ResolvedCall<? extends CallableDescriptor> resolvedCall =
                            CallUtilKt.getResolvedCall(callExpression, context.trace.getBindingContext());

                    if (resolvedCall != null &&
                        !KtPsiUtil.isLabeledFunctionLiteral((KtFunctionLiteral) labelTargetElement) &&
                        Objects.equals(
                                DescriptorUtilsKt.fqNameOrNull(resolvedCall.getResultingDescriptor()),
                                SuspendFunctionTypeUtilKt.KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME
                        )
                    ) {
                        context.trace.report(RETURN_FOR_BUILT_IN_SUSPEND.on(expression));
                    }
                }
            }
        }

        if (returnedExpression != null) {
            if (newInferenceLambdaInfo != null) {
                LambdaContextInfo contextInfo;
                KtExpression deparenthesizedReturnExpression = KtPsiUtil.deparenthesize(returnedExpression);
                if (deparenthesizedReturnExpression instanceof KtLambdaExpression ||
                    deparenthesizedReturnExpression instanceof KtCallableReferenceExpression
                ) {
                    contextInfo = new LambdaContextInfo(
                            new KotlinTypeInfo(DONT_CARE, context.dataFlowInfo),
                            null,
                            context.scope,
                            context.trace
                    );
                } else {
                    KotlinTypeInfo result = facade
                            .getTypeInfo(returnedExpression, context.replaceExpectedType(newInferenceLambdaInfo.getExpectedType())
                                    .replaceContextDependency(newInferenceLambdaInfo.getContextDependency()));
                    contextInfo = new LambdaContextInfo(result, null, context.scope, context.trace);
                }
                newInferenceLambdaInfo.getReturnStatements().add(new kotlin.Pair<>(expression, contextInfo));
            }
            else {
                facade.getTypeInfo(returnedExpression, context.replaceExpectedType(expectedType).replaceContextDependency(INDEPENDENT));
            }
        }
        else {
            // for lambda with implicit return type Unit
            if (!noExpectedType(expectedType) && !KotlinBuiltIns.isUnit(expectedType) && !isDontCarePlaceholder(expectedType)) {
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, expectedType));
            }
            if (newInferenceLambdaInfo != null) {
                newInferenceLambdaInfo.getReturnStatements().add(new kotlin.Pair<>(expression, null));
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
