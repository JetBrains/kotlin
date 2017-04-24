/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtilsKt;
import org.jetbrains.kotlin.incremental.KotlinLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.smartcasts.*;
import org.jetbrains.kotlin.resolve.constants.*;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.KotlinTypeKt;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.*;

public class DataFlowAnalyzer {

    private final Iterable<AdditionalTypeChecker> additionalTypeCheckers;
    private final ConstantExpressionEvaluator constantExpressionEvaluator;
    private final KotlinBuiltIns builtIns;
    private final SmartCastManager smartCastManager;
    private final ExpressionTypingFacade facade;
    private final LanguageVersionSettings languageVersionSettings;

    public DataFlowAnalyzer(
            @NotNull Iterable<AdditionalTypeChecker> additionalTypeCheckers,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull SmartCastManager smartCastManager,
            @NotNull ExpressionTypingFacade facade,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        this.additionalTypeCheckers = additionalTypeCheckers;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.builtIns = builtIns;
        this.smartCastManager = smartCastManager;
        this.facade = facade;
        this.languageVersionSettings = languageVersionSettings;
    }

    // NB: use this method only for functions from 'Any'
    @Nullable
    private static FunctionDescriptor getOverriddenDescriptorFromClass(@NotNull FunctionDescriptor descriptor) {
        if (descriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) return descriptor;
        Collection<? extends FunctionDescriptor> overriddenDescriptors = descriptor.getOverriddenDescriptors();
        if (overriddenDescriptors.isEmpty()) return descriptor;
        for (FunctionDescriptor overridden : overriddenDescriptors) {
            DeclarationDescriptor containingDeclaration = overridden.getContainingDeclaration();
            if (DescriptorUtils.isClass(containingDeclaration) || DescriptorUtils.isObject(containingDeclaration)) {
                // Exactly one class should exist in the list
                return getOverriddenDescriptorFromClass(overridden);
            }
        }
        return null;
    }

    private static boolean typeHasOverriddenEquals(@NotNull KotlinType type, @NotNull KtElement lookupElement) {
        Collection<SimpleFunctionDescriptor> members = type.getMemberScope().getContributedFunctions(
                OperatorNameConventions.EQUALS, new KotlinLookupLocation(lookupElement));
        for (FunctionDescriptor member : members) {
            KotlinType returnType = member.getReturnType();
            if (returnType == null || !KotlinBuiltIns.isBoolean(returnType)) continue;
            if (member.getValueParameters().size() != 1) continue;
            KotlinType parameterType = member.getValueParameters().iterator().next().getType();
            if (!KotlinBuiltIns.isNullableAny(parameterType)) continue;
            FunctionDescriptor fromSuperClass = getOverriddenDescriptorFromClass(member);
            if (fromSuperClass == null) return false;
            ClassifierDescriptor superClassDescriptor = (ClassifierDescriptor) fromSuperClass.getContainingDeclaration();
            // We should have override fun in class other than Any (to prove unknown behaviour)
            return !KotlinBuiltIns.isAnyOrNullableAny(superClassDescriptor.getDefaultType());
        }
        return false;
    }

    // Returns true if we can prove that 'type' has equals method from 'Any' base type
    public static boolean typeHasEqualsFromAny(@NotNull KotlinType type, @NotNull KtElement lookupElement) {
        TypeConstructor constructor = type.getConstructor();
        // Subtypes can override equals for non-final types
        if (!constructor.isFinal()) return false;
        // check whether 'equals' is overriden
        return !typeHasOverriddenEquals(type, lookupElement);
    }

    @NotNull
    public DataFlowInfo extractDataFlowInfoFromCondition(
            @Nullable KtExpression condition,
            boolean conditionValue,
            ExpressionTypingContext context
    ) {
        if (condition == null) return context.dataFlowInfo;
        Ref<DataFlowInfo> result = new Ref<>(null);
        condition.accept(new KtVisitorVoid() {
            @Override
            public void visitIsExpression(@NotNull KtIsExpression expression) {
                if (conditionValue && !expression.isNegated() || !conditionValue && expression.isNegated()) {
                    result.set(context.trace.get(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression));
                }
            }

            @Override
            public void visitBinaryExpression(@NotNull KtBinaryExpression expression) {
                IElementType operationToken = expression.getOperationToken();
                if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationToken)) {
                    DataFlowInfo dataFlowInfo = extractDataFlowInfoFromCondition(expression.getLeft(), conditionValue, context);
                    KtExpression expressionRight = expression.getRight();
                    if (expressionRight != null) {
                        boolean and = operationToken == KtTokens.ANDAND;
                        DataFlowInfo rightInfo = extractDataFlowInfoFromCondition(
                                expressionRight, conditionValue,
                                and == conditionValue ? context.replaceDataFlowInfo(dataFlowInfo) : context
                        );
                        if (and == conditionValue) { // this means: and && conditionValue || !and && !conditionValue
                            dataFlowInfo = dataFlowInfo.and(rightInfo);
                        }
                        else {
                            dataFlowInfo = dataFlowInfo.or(rightInfo);
                        }
                    }
                    result.set(dataFlowInfo);
                }
                else  {
                    DataFlowInfo expressionFlowInfo = facade.getTypeInfo(expression, context).getDataFlowInfo();
                    KtExpression left = expression.getLeft();
                    if (left == null) return;
                    KtExpression right = expression.getRight();
                    if (right == null) return;

                    KotlinType lhsType = context.trace.getBindingContext().getType(left);
                    if (lhsType == null) return;
                    KotlinType rhsType = context.trace.getBindingContext().getType(right);
                    if (rhsType == null) return;

                    DataFlowValue leftValue = DataFlowValueFactory.createDataFlowValue(left, lhsType, context);
                    DataFlowValue rightValue = DataFlowValueFactory.createDataFlowValue(right, rhsType, context);

                    Boolean equals = null;
                    if (operationToken == KtTokens.EQEQ || operationToken == KtTokens.EQEQEQ) {
                        equals = true;
                    }
                    else if (operationToken == KtTokens.EXCLEQ || operationToken == KtTokens.EXCLEQEQEQ) {
                        equals = false;
                    }
                    if (equals != null) {
                        if (equals == conditionValue) { // this means: equals && conditionValue || !equals && !conditionValue
                            boolean identityEquals = operationToken == KtTokens.EQEQEQ ||
                                                     operationToken == KtTokens.EXCLEQEQEQ ||
                                                     typeHasEqualsFromAny(lhsType, condition);
                            result.set(context.dataFlowInfo
                                               .equate(leftValue, rightValue, identityEquals, languageVersionSettings)
                                               .and(expressionFlowInfo));
                        }
                        else {
                            result.set(context.dataFlowInfo
                                               .disequate(leftValue, rightValue, languageVersionSettings)
                                               .and(expressionFlowInfo));
                        }
                    }
                    else {
                        result.set(expressionFlowInfo);
                    }
                }
            }

            @Override
            public void visitUnaryExpression(@NotNull KtUnaryExpression expression) {
                IElementType operationTokenType = expression.getOperationReference().getReferencedNameElementType();
                if (operationTokenType == KtTokens.EXCL) {
                    KtExpression baseExpression = expression.getBaseExpression();
                    if (baseExpression != null) {
                        result.set(extractDataFlowInfoFromCondition(baseExpression, !conditionValue, context));
                    }
                }
                else {
                    visitExpression(expression);
                }
            }

            @Override
            public void visitExpression(@NotNull KtExpression expression) {
                // In fact, everything is taken from trace here
                result.set(facade.getTypeInfo(expression, context).getDataFlowInfo());
            }

            @Override
            public void visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression) {
                KtExpression body = expression.getExpression();
                if (body != null) {
                    body.accept(this);
                }
            }
        });
        if (result.get() == null) {
            return context.dataFlowInfo;
        }
        return context.dataFlowInfo.and(result.get());
    }

    @Nullable
    public KotlinType checkType(@Nullable KotlinType expressionType, @NotNull KtExpression expression, @NotNull ResolutionContext context) {
        return checkType(expressionType, expression, context, null);
    }

    @NotNull
    public KotlinTypeInfo checkType(@NotNull KotlinTypeInfo typeInfo, @NotNull KtExpression expression, @NotNull ResolutionContext context) {
        return typeInfo.replaceType(checkType(typeInfo.getType(), expression, context));
    }

    @NotNull
    private KotlinType checkTypeInternal(
            @NotNull KotlinType expressionType,
            @NotNull KtExpression expression,
            @NotNull ResolutionContext c,
            @NotNull Ref<Boolean> hasError
    ) {
        if (noExpectedType(c.expectedType) || !c.expectedType.getConstructor().isDenotable() ||
            KotlinTypeChecker.DEFAULT.isSubtypeOf(expressionType, c.expectedType)) {
            return expressionType;
        }

        if (expression instanceof KtConstantExpression) {
            ConstantValue<?> constantValue = constantExpressionEvaluator.evaluateToConstantValue(expression, c.trace, c.expectedType);
            boolean error = new CompileTimeConstantChecker(c, builtIns, true)
                    .checkConstantExpressionType(constantValue, (KtConstantExpression) expression, c.expectedType);
            hasError.set(error);
            return expressionType;
        }

        if (expression instanceof KtWhenExpression) {
            // No need in additional check because type mismatch is already reported for entries
            return expressionType;
        }

        SmartCastResult castResult = checkPossibleCast(expressionType, expression, c);
        if (castResult != null) return castResult.getResultType();

        if (!DiagnosticUtilsKt.reportTypeMismatchDueToTypeProjection(c, expression, c.expectedType, expressionType) &&
            !DiagnosticUtilsKt.reportTypeMismatchDueToScalaLikeNamedFunctionSyntax(c, expression, c.expectedType, expressionType)) {
            c.trace.report(TYPE_MISMATCH.on(expression, c.expectedType, expressionType));
        }
        hasError.set(true);
        return expressionType;
    }

    @Nullable
    public KotlinType checkType(
            @Nullable KotlinType expressionType,
            @NotNull KtExpression expressionToCheck,
            @NotNull ResolutionContext c,
            @Nullable Ref<Boolean> hasError
    ) {
        if (hasError == null) {
            hasError = Ref.create(false);
        }
        else {
            hasError.set(false);
        }

        KtExpression expression = KtPsiUtil.safeDeparenthesize(expressionToCheck);
        recordExpectedType(c.trace, expression, c.expectedType);

        if (expressionType == null) return null;

        KotlinType result = checkTypeInternal(expressionType, expression, c, hasError);
        if (Boolean.FALSE.equals(hasError.get())) {
            for (AdditionalTypeChecker checker : additionalTypeCheckers) {
                checker.checkType(expression, expressionType, result, c);
            }
        }

        return result;
    }

    @Nullable
    public static SmartCastResult checkPossibleCast(
            @NotNull KotlinType expressionType,
            @NotNull KtExpression expression,
            @NotNull ResolutionContext c
    ) {
        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, expressionType, c);

        return SmartCastManager.Companion.checkAndRecordPossibleCast(dataFlowValue, c.expectedType, expression, c, null, false);
    }

    public void recordExpectedType(@NotNull BindingTrace trace, @NotNull KtExpression expression, @NotNull KotlinType expectedType) {
        if (expectedType != NO_EXPECTED_TYPE) {
            KotlinType normalizeExpectedType = expectedType == UNIT_EXPECTED_TYPE ? builtIns.getUnitType() : expectedType;
            trace.record(BindingContext.EXPECTED_EXPRESSION_TYPE, expression, normalizeExpectedType);
        }
    }

    @Nullable
    public KotlinType checkStatementType(@NotNull KtExpression expression, @NotNull ResolutionContext context) {
        if (!noExpectedType(context.expectedType) && !KotlinBuiltIns.isUnit(context.expectedType) &&
            !KotlinTypeKt.isError(context.expectedType)) {
            context.trace.report(EXPECTED_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return builtIns.getUnitType();
    }

    @NotNull
    public static KotlinTypeInfo illegalStatementType(@NotNull KtExpression expression, @NotNull ExpressionTypingContext context, @NotNull ExpressionTypingInternals facade) {
        facade.checkStatementType(
                expression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT));
        context.trace.report(EXPRESSION_EXPECTED.on(expression, expression));
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @NotNull
    public static Collection<KotlinType> getAllPossibleTypes(
            @NotNull KtExpression expression,
            @NotNull KotlinType type,
            @NotNull ResolutionContext c
    ) {
        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, type, c);
        Collection<KotlinType> possibleTypes = Sets.newHashSet(type);
        possibleTypes.addAll(c.dataFlowInfo.getStableTypes(dataFlowValue));
        return possibleTypes;
    }

    @NotNull
    public KotlinTypeInfo createCheckedTypeInfo(
            @Nullable KotlinType type,
            @NotNull ResolutionContext<?> context,
            @NotNull KtExpression expression
    ) {
        return checkType(TypeInfoFactoryKt.createTypeInfo(type, context), expression, context);
    }

    @NotNull
    public KotlinTypeInfo createCompileTimeConstantTypeInfo(
            @NotNull CompileTimeConstant<?> value,
            @NotNull KtExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        KotlinType expressionType;
        if (value instanceof IntegerValueTypeConstant) {
            IntegerValueTypeConstant integerValueTypeConstant = (IntegerValueTypeConstant) value;
            if (context.contextDependency == INDEPENDENT) {
                expressionType = integerValueTypeConstant.getType(context.expectedType);
                constantExpressionEvaluator.updateNumberType(expressionType, expression, context.statementFilter, context.trace);
            }
            else {
                expressionType = integerValueTypeConstant.getUnknownIntegerType();
            }
        }
        else {
            expressionType = ((TypedCompileTimeConstant<?>) value).getType();
        }

        return createCheckedTypeInfo(expressionType, context, expression);
    }
}
