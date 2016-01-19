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

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cfg.WhenChecker;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastResult;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import java.util.Collections;
import java.util.Set;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isBoolean;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.newWritableScopeImpl;

public class PatternMatchingTypingVisitor extends ExpressionTypingVisitor {
    protected PatternMatchingTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public KotlinTypeInfo visitIsExpression(@NotNull KtIsExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType
                                            .replaceExpectedType(NO_EXPECTED_TYPE)
                                            .replaceContextDependency(INDEPENDENT);
        KtExpression leftHandSide = expression.getLeftHandSide();
        KotlinTypeInfo typeInfo = facade.safeGetTypeInfo(leftHandSide, context.replaceScope(context.scope));
        KotlinType knownType = typeInfo.getType();
        if (expression.getTypeReference() != null) {
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(leftHandSide, knownType, context);
            DataFlowInfo conditionInfo = checkTypeForIs(context, knownType, expression.getTypeReference(), dataFlowValue).thenInfo;
            DataFlowInfo newDataFlowInfo = conditionInfo.and(typeInfo.getDataFlowInfo());
            context.trace.record(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression, newDataFlowInfo);
        }
        return components.dataFlowAnalyzer.checkType(typeInfo.replaceType(components.builtIns.getBooleanType()), expression, contextWithExpectedType);
    }

    @Override
    public KotlinTypeInfo visitWhenExpression(@NotNull KtWhenExpression expression, ExpressionTypingContext context) {
        return visitWhenExpression(expression, context, false);
    }

    public KotlinTypeInfo visitWhenExpression(KtWhenExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        WhenChecker.checkDeprecatedWhenSyntax(contextWithExpectedType.trace, expression);
        WhenChecker.checkReservedPrefix(contextWithExpectedType.trace, expression);

        components.dataFlowAnalyzer.recordExpectedType(contextWithExpectedType.trace, expression, contextWithExpectedType.expectedType);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        // TODO :change scope according to the bound value in the when header
        KtExpression subjectExpression = expression.getSubjectExpression();

        KotlinType subjectType;
        boolean loopBreakContinuePossible = false;
        if (subjectExpression == null) {
            subjectType = ErrorUtils.createErrorType("Unknown type");
        }
        else {
            KotlinTypeInfo typeInfo = facade.safeGetTypeInfo(subjectExpression, context);
            loopBreakContinuePossible = typeInfo.getJumpOutPossible();
            subjectType = typeInfo.getType();
            assert subjectType != null;
            if (TypeUtils.isNullableType(subjectType) && !WhenChecker.containsNullCase(expression, context.trace.getBindingContext())) {
                TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace, "Temporary trace for when subject nullability");
                ExpressionTypingContext subjectContext =
                        context.replaceExpectedType(TypeUtils.makeNotNullable(subjectType)).replaceBindingTrace(trace);
                SmartCastResult castResult = components.dataFlowAnalyzer.checkPossibleCast(
                        subjectType, KtPsiUtil.safeDeparenthesize(subjectExpression), subjectContext
                );
                if (castResult != null && castResult.isCorrect()) {
                    trace.commit();
                }
            }
            context = context.replaceDataFlowInfo(typeInfo.getDataFlowInfo());
        }
        DataFlowValue subjectDataFlowValue = subjectExpression != null
                ? DataFlowValueFactory.createDataFlowValue(subjectExpression, subjectType, context)
                : DataFlowValue.nullValue(components.builtIns);

        // TODO : exhaustive patterns

        Set<KotlinType> expressionTypes = Sets.newHashSet();
        DataFlowInfo commonDataFlowInfo = null;
        DataFlowInfo elseDataFlowInfo = context.dataFlowInfo;
        DataFlowValue whenValue = DataFlowValueFactory.createDataFlowValue(expression, components.builtIns.getNullableAnyType(), context);
        for (KtWhenEntry whenEntry : expression.getEntries()) {
            DataFlowInfos infosForCondition = getDataFlowInfosForEntryCondition(
                    whenEntry, context.replaceDataFlowInfo(elseDataFlowInfo), subjectExpression, subjectType, subjectDataFlowValue);
            elseDataFlowInfo = elseDataFlowInfo.and(infosForCondition.elseInfo);

            KtExpression bodyExpression = whenEntry.getExpression();
            if (bodyExpression != null) {
                LexicalWritableScope scopeToExtend = newWritableScopeImpl(context, LexicalScopeKind.WHEN);
                ExpressionTypingContext newContext = contextWithExpectedType
                        .replaceScope(scopeToExtend).replaceDataFlowInfo(infosForCondition.thenInfo).replaceContextDependency(INDEPENDENT);
                CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
                KotlinTypeInfo typeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                        scopeToExtend, Collections.singletonList(bodyExpression), coercionStrategy, newContext);
                loopBreakContinuePossible |= typeInfo.getJumpOutPossible();
                KotlinType type = typeInfo.getType();
                if (type != null) {
                    expressionTypes.add(type);
                    DataFlowValue entryValue = DataFlowValueFactory.createDataFlowValue(bodyExpression, type, context);
                    typeInfo = typeInfo.replaceDataFlowInfo(typeInfo.getDataFlowInfo().assign(whenValue, entryValue));
                }
                if (commonDataFlowInfo == null) {
                    commonDataFlowInfo = typeInfo.getDataFlowInfo();
                }
                else {
                    commonDataFlowInfo = commonDataFlowInfo.or(typeInfo.getDataFlowInfo());
                }
            }
        }

        boolean isExhaustive = WhenChecker.isWhenExhaustive(expression, context.trace);
        if (commonDataFlowInfo == null) {
            commonDataFlowInfo = context.dataFlowInfo;
        }
        else if (expression.getElseExpression() == null && !isExhaustive) {
            // Without else expression in non-exhaustive when, we *must* take initial data flow info into account,
            // because data flow can bypass all when branches in this case
            commonDataFlowInfo = commonDataFlowInfo.or(context.dataFlowInfo);
        }

        KotlinType resultType = expressionTypes.isEmpty() ? null : CommonSupertypes.commonSupertype(expressionTypes);
        if (resultType != null) {
            DataFlowValue resultValue = DataFlowValueFactory.createDataFlowValue(expression, resultType, context);
            commonDataFlowInfo = commonDataFlowInfo.assign(resultValue, whenValue);
            if (isExhaustive && expression.getElseExpression() == null && KotlinBuiltIns.isNothing(resultType)) {
                context.trace.record(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, expression);
            }
            resultType = components.dataFlowAnalyzer.checkType(resultType, expression, contextWithExpectedType);
        }
        return TypeInfoFactoryKt.createTypeInfo(resultType,
                                                commonDataFlowInfo,
                                                loopBreakContinuePossible,
                                                contextWithExpectedType.dataFlowInfo);
    }

    @NotNull
    private DataFlowInfos getDataFlowInfosForEntryCondition(
            @NotNull KtWhenEntry whenEntry,
            @NotNull ExpressionTypingContext context,
            @Nullable KtExpression subjectExpression,
            @NotNull KotlinType subjectType,
            @NotNull DataFlowValue subjectDataFlowValue
    ) {
        if (whenEntry.isElse()) {
            return new DataFlowInfos(context.dataFlowInfo);
        }

        DataFlowInfos infos = null;
        ExpressionTypingContext contextForCondition = context;
        for (KtWhenCondition condition : whenEntry.getConditions()) {
            DataFlowInfos conditionInfos = checkWhenCondition(subjectExpression, subjectType, condition,
                                                              contextForCondition, subjectDataFlowValue);
            if (infos != null) {
                infos = new DataFlowInfos(infos.thenInfo.or(conditionInfos.thenInfo), infos.elseInfo.and(conditionInfos.elseInfo));
            }
            else {
                infos = conditionInfos;
            }
            contextForCondition = contextForCondition.replaceDataFlowInfo(conditionInfos.elseInfo);
        }
        return infos != null ? infos : new DataFlowInfos(context.dataFlowInfo);
    }

    private DataFlowInfos checkWhenCondition(
            @Nullable final KtExpression subjectExpression,
            final KotlinType subjectType,
            KtWhenCondition condition,
            final ExpressionTypingContext context,
            final DataFlowValue subjectDataFlowValue
    ) {
        final Ref<DataFlowInfos> newDataFlowInfo = new Ref<DataFlowInfos>(noChange(context));
        condition.accept(new KtVisitorVoid() {
            @Override
            public void visitWhenConditionInRange(@NotNull KtWhenConditionInRange condition) {
                KtExpression rangeExpression = condition.getRangeExpression();
                if (rangeExpression == null) return;
                if (subjectExpression == null) {
                    context.trace.report(EXPECTED_CONDITION.on(condition));
                    DataFlowInfo dataFlowInfo = facade.getTypeInfo(rangeExpression, context).getDataFlowInfo();
                    newDataFlowInfo.set(new DataFlowInfos(dataFlowInfo, dataFlowInfo));
                    return;
                }
                ValueArgument argumentForSubject = CallMaker.makeExternalValueArgument(subjectExpression);
                KotlinTypeInfo typeInfo = facade.checkInExpression(condition, condition.getOperationReference(),
                                                                   argumentForSubject, rangeExpression, context);
                DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
                newDataFlowInfo.set(new DataFlowInfos(dataFlowInfo, dataFlowInfo));
                KotlinType type = typeInfo.getType();
                if (type == null || !isBoolean(type)) {
                    context.trace.report(TYPE_MISMATCH_IN_RANGE.on(condition));
                }
            }

            @Override
            public void visitWhenConditionIsPattern(@NotNull KtWhenConditionIsPattern condition) {
                if (subjectExpression == null) {
                    context.trace.report(EXPECTED_CONDITION.on(condition));
                }
                if (condition.getTypeReference() != null) {
                    DataFlowInfos result = checkTypeForIs(context, subjectType, condition.getTypeReference(), subjectDataFlowValue);
                    if (condition.isNegated()) {
                        newDataFlowInfo.set(new DataFlowInfos(result.elseInfo, result.thenInfo));
                    }
                    else {
                        newDataFlowInfo.set(result);
                    }
                }
            }

            @Override
            public void visitWhenConditionWithExpression(@NotNull KtWhenConditionWithExpression condition) {
                KtExpression expression = condition.getExpression();
                if (expression != null) {
                    newDataFlowInfo.set(checkTypeForExpressionCondition(context, expression, subjectType, subjectExpression == null,
                                                                        subjectDataFlowValue));
                }
            }

            @Override
            public void visitKtElement(@NotNull KtElement element) {
                context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
            }
        });
        return newDataFlowInfo.get();
    }

    private static class DataFlowInfos {
        private final DataFlowInfo thenInfo;
        private final DataFlowInfo elseInfo;

        private DataFlowInfos(DataFlowInfo thenInfo, DataFlowInfo elseInfo) {
            this.thenInfo = thenInfo;
            this.elseInfo = elseInfo;
        }

        private DataFlowInfos(DataFlowInfo info) {
            this(info, info);
        }
    }

    private DataFlowInfos checkTypeForExpressionCondition(
            ExpressionTypingContext context,
            KtExpression expression,
            KotlinType subjectType,
            boolean conditionExpected,
            DataFlowValue subjectDataFlowValue
    ) {
        if (expression == null) {
            return noChange(context);
        }
        KotlinTypeInfo typeInfo = facade.getTypeInfo(expression, context);
        KotlinType type = typeInfo.getType();
        if (type == null) {
            return noChange(context);
        }
        context = context.replaceDataFlowInfo(typeInfo.getDataFlowInfo());
        if (conditionExpected) {
            KotlinType booleanType = components.builtIns.getBooleanType();
            if (!KotlinTypeChecker.DEFAULT.equalTypes(booleanType, type)) {
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(expression, type));
            }
            else {
                DataFlowInfo ifInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, true, context);
                DataFlowInfo elseInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, false, context);
                return new DataFlowInfos(ifInfo, elseInfo);
            }
            return noChange(context);
        }
        checkTypeCompatibility(context, type, subjectType, expression);
        DataFlowValue expressionDataFlowValue =
                DataFlowValueFactory.createDataFlowValue(expression, type, context);
        DataFlowInfos result = noChange(context);
        result = new DataFlowInfos(
                result.thenInfo.equate(subjectDataFlowValue, expressionDataFlowValue),
                result.elseInfo.disequate(subjectDataFlowValue, expressionDataFlowValue)
        );
        return result;
    }

    private DataFlowInfos checkTypeForIs(
            ExpressionTypingContext context,
            KotlinType subjectType,
            KtTypeReference typeReferenceAfterIs,
            DataFlowValue subjectDataFlowValue
    ) {
        if (typeReferenceAfterIs == null) {
            return noChange(context);
        }
        TypeResolutionContext typeResolutionContext = new TypeResolutionContext(context.scope, context.trace, true, /*allowBareTypes=*/ true);
        PossiblyBareType possiblyBareTarget = components.typeResolver.resolvePossiblyBareType(typeResolutionContext, typeReferenceAfterIs);
        KotlinType targetType = TypeReconstructionUtil.reconstructBareType(typeReferenceAfterIs, possiblyBareTarget, subjectType, context.trace, components.builtIns);

        if (DynamicTypesKt.isDynamic(targetType)) {
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(typeReferenceAfterIs));
        }
        ClassDescriptor targetDescriptor = TypeUtils.getClassDescriptor(targetType);
        if (targetDescriptor != null && DescriptorUtils.isEnumEntry(targetDescriptor)) {
            context.trace.report(IS_ENUM_ENTRY.on(typeReferenceAfterIs));
        }

        if (!subjectType.isMarkedNullable() && targetType.isMarkedNullable()) {
            KtTypeElement element = typeReferenceAfterIs.getTypeElement();
            assert element instanceof KtNullableType : "element must be instance of " + KtNullableType.class.getName();
            KtNullableType nullableType = (KtNullableType) element;
            context.trace.report(Errors.USELESS_NULLABLE_CHECK.on(nullableType));
        }
        checkTypeCompatibility(context, targetType, subjectType, typeReferenceAfterIs);
        if (CastDiagnosticsUtil.isCastErased(subjectType, targetType, KotlinTypeChecker.DEFAULT)) {
            context.trace.report(Errors.CANNOT_CHECK_FOR_ERASED.on(typeReferenceAfterIs, targetType));
        }
        return new DataFlowInfos(context.dataFlowInfo.establishSubtyping(subjectDataFlowValue, targetType), context.dataFlowInfo);
    }

    private static DataFlowInfos noChange(ExpressionTypingContext context) {
        return new DataFlowInfos(context.dataFlowInfo, context.dataFlowInfo);
    }

    /*
     * (a: SubjectType) is Type
     */
    private static void checkTypeCompatibility(
            @NotNull ExpressionTypingContext context,
            @Nullable KotlinType type,
            @NotNull KotlinType subjectType,
            @NotNull KtElement reportErrorOn
    ) {
        // TODO : Take smart casts into account?
        if (type == null) {
            return;
        }
        if (TypeIntersector.isIntersectionEmpty(type, subjectType)) {
            context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType));
            return;
        }

        // check if the pattern is essentially a 'null' expression
        if (KotlinBuiltIns.isNullableNothing(type) && !TypeUtils.isNullableType(subjectType)) {
            context.trace.report(SENSELESS_NULL_IN_WHEN.on(reportErrorOn));
        }
    }
}
