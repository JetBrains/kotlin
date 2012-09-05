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
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.Collections;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.newWritableScopeImpl;

/**
 * @author abreslav
 */
public class PatternMatchingTypingVisitor extends ExpressionTypingVisitor {
    protected PatternMatchingTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetTypeInfo visitIsExpression(JetIsExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression leftHandSide = expression.getLeftHandSide();
        JetType knownType = facade.safeGetTypeInfo(leftHandSide, context.replaceScope(context.scope)).getType();
        if (expression.getTypeRef() != null) {
            DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(leftHandSide, knownType, context.trace.getBindingContext());
            DataFlowInfo newDataFlowInfo = checkTypeForIs(context, knownType, expression.getTypeRef(), dataFlowValue).thenInfo;
            context.trace.record(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression, newDataFlowInfo);
        }
        return DataFlowUtils.checkType(JetStandardLibrary.getInstance().getBooleanType(), expression, contextWithExpectedType, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitWhenExpression(final JetWhenExpression expression, ExpressionTypingContext context) {
        return visitWhenExpression(expression, context, false);
    }

    public JetTypeInfo visitWhenExpression(final JetWhenExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        // TODO :change scope according to the bound value in the when header
        final JetExpression subjectExpression = expression.getSubjectExpression();

        final JetType subjectType = subjectExpression != null
                                    ? context.expressionTypingServices.safeGetType(context.scope, subjectExpression, TypeUtils.NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace)
                                    : ErrorUtils.createErrorType("Unknown type");
        final DataFlowValue variableDescriptor = subjectExpression != null ? DataFlowValueFactory.INSTANCE.createDataFlowValue(subjectExpression, subjectType, context.trace.getBindingContext()) : DataFlowValue.NULL;

        // TODO : exhaustive patterns

        Set<JetType> expressionTypes = Sets.newHashSet();
        DataFlowInfo commonDataFlowInfo = null;
        DataFlowInfo elseDataFlowInfo = context.dataFlowInfo;
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            JetWhenCondition[] conditions = whenEntry.getConditions();
            DataFlowInfo newDataFlowInfo;
            WritableScope scopeToExtend;
            if (whenEntry.isElse()) {
                scopeToExtend = newWritableScopeImpl(context, "Scope extended in when-else entry");
                newDataFlowInfo = elseDataFlowInfo;
            }
            else if (conditions.length == 1) {
                scopeToExtend = newWritableScopeImpl(context, "Scope extended in when entry");
                newDataFlowInfo = context.dataFlowInfo;
                JetWhenCondition condition = conditions[0];
                if (condition != null) {
                    DataFlowInfos infos = checkWhenCondition(
                            subjectExpression, subjectExpression == null,
                            subjectType, condition,
                            context, variableDescriptor);
                    newDataFlowInfo = infos.thenInfo;
                    elseDataFlowInfo = elseDataFlowInfo.and(infos.elseInfo);
                }
            }
            else {
                scopeToExtend = newWritableScopeImpl(context, "pattern matching"); // We don't write to this scope
                newDataFlowInfo = null;
                for (JetWhenCondition condition : conditions) {
                    DataFlowInfos infos = checkWhenCondition(subjectExpression, subjectExpression == null, subjectType, condition,
                                                             context, variableDescriptor);
                    if (newDataFlowInfo == null) {
                        newDataFlowInfo = infos.thenInfo;
                    }
                    else {
                        newDataFlowInfo = newDataFlowInfo.or(infos.thenInfo);
                    }
                    elseDataFlowInfo = elseDataFlowInfo.and(infos.elseInfo);
                }
                if (newDataFlowInfo == null) {
                    newDataFlowInfo = context.dataFlowInfo;
                }
            }
            JetExpression bodyExpression = whenEntry.getExpression();
            if (bodyExpression != null) {
                ExpressionTypingContext newContext = contextWithExpectedType.replaceScope(scopeToExtend).replaceDataFlowInfo(newDataFlowInfo);
                CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
                JetTypeInfo typeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(scopeToExtend, Collections.singletonList(bodyExpression), coercionStrategy, newContext, context.trace);
                JetType type = typeInfo.getType();
                if (type != null) {
                    expressionTypes.add(type);
                }
                if (commonDataFlowInfo == null) {
                    commonDataFlowInfo = typeInfo.getDataFlowInfo();
                }
                else {
                    commonDataFlowInfo = commonDataFlowInfo.or(typeInfo.getDataFlowInfo());
                }
            }
        }

        if (commonDataFlowInfo == null) {
            commonDataFlowInfo = context.dataFlowInfo;
        }

        if (!expressionTypes.isEmpty()) {
            return DataFlowUtils.checkImplicitCast(CommonSupertypes.commonSupertype(expressionTypes), expression, contextWithExpectedType, isStatement, commonDataFlowInfo);
        }
        return JetTypeInfo.create(null, commonDataFlowInfo);
    }

    private DataFlowInfos checkWhenCondition(
            @Nullable final JetExpression subjectExpression,
            final boolean expectedCondition,
            final JetType subjectType,
            JetWhenCondition condition,
            final ExpressionTypingContext context,
            final DataFlowValue... subjectVariables
    ) {
        final Ref<DataFlowInfos> newDataFlowInfo = new Ref<DataFlowInfos>(noChange(context));
        condition.accept(new JetVisitorVoid() {

            @Override
            public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
                JetExpression rangeExpression = condition.getRangeExpression();
                if (rangeExpression == null) return;
                if (expectedCondition) {
                    context.trace.report(EXPECTED_CONDITION.on(condition));
                    facade.getTypeInfo(rangeExpression, context);
                    return;
                }
                if (!facade.checkInExpression(condition, condition.getOperationReference(), subjectExpression, rangeExpression, context)) {
                    context.trace.report(TYPE_MISMATCH_IN_RANGE.on(condition));
                }
            }

            @Override
            public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
                if (expectedCondition) {
                    context.trace.report(EXPECTED_CONDITION.on(condition));
                }
                if (condition.getTypeRef() != null) {
                    DataFlowInfos result = checkTypeForIs(context, subjectType, condition.getTypeRef(), subjectVariables);
                    if (condition.isNegated()) {
                        newDataFlowInfo.set(new DataFlowInfos(result.elseInfo, result.thenInfo));
                    }
                    else {
                        newDataFlowInfo.set(result);
                    }
                }
            }

            @Override
            public void visitWhenConditionWithExpression(JetWhenConditionWithExpression condition) {
                JetExpression expression = condition.getExpression();
                if (expression != null) {
                    newDataFlowInfo.set(checkTypeForExpressionCondition(context, expression, subjectType, subjectExpression == null,
                                                                        subjectVariables));
                }
            }

            @Override
            public void visitJetElement(JetElement element) {
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
    }

    private DataFlowInfos checkTypeForExpressionCondition(
            ExpressionTypingContext context,
            JetExpression expression,
            JetType subjectType,
            boolean conditionExpected,
            DataFlowValue... subjectVariables
    ) {
        if (expression == null) {
            return noChange(context);
        }
        JetTypeInfo typeInfo = facade.getTypeInfo(expression, context);
        JetType type = typeInfo.getType();
        if (type == null) {
            return noChange(context);
        }
        if (conditionExpected) {
            JetType booleanType = JetStandardLibrary.getInstance().getBooleanType();
            if (!JetTypeChecker.INSTANCE.equalTypes(booleanType, type)) {
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(expression, type));
            }
            else {
                DataFlowInfo ifInfo = DataFlowUtils.extractDataFlowInfoFromCondition(expression, true, context);
                DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(expression, false, context);
                return new DataFlowInfos(ifInfo, elseInfo);
            }
            return noChange(context);
        }
        checkTypeCompatibility(context, type, subjectType, expression);
        DataFlowValue expressionDataFlowValue =
                DataFlowValueFactory.INSTANCE.createDataFlowValue(expression, type, context.trace.getBindingContext());
        DataFlowInfos result = noChange(context);
        for (DataFlowValue subjectVariable : subjectVariables) {
            result = new DataFlowInfos(
                    result.thenInfo.equate(subjectVariable, expressionDataFlowValue),
                    result.elseInfo.disequate(subjectVariable, expressionDataFlowValue)
            );
        }
        return result;
    }

    private static DataFlowInfos checkTypeForIs(
            ExpressionTypingContext context,
            JetType subjectType,
            JetTypeReference typeReferenceAfterIs,
            DataFlowValue... subjectVariables
    ) {
        if (typeReferenceAfterIs == null) {
            return noChange(context);
        }
        JetType type = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReferenceAfterIs, context.trace, true);
        checkTypeCompatibility(context, type, subjectType, typeReferenceAfterIs);
        if (BasicExpressionTypingVisitor.isCastErased(subjectType, type, JetTypeChecker.INSTANCE)) {
            context.trace.report(Errors.CANNOT_CHECK_FOR_ERASED.on(typeReferenceAfterIs, type));
        }
        return new DataFlowInfos(context.dataFlowInfo.establishSubtyping(subjectVariables, type), context.dataFlowInfo);
    }

    private static DataFlowInfos noChange(ExpressionTypingContext context) {
        return new DataFlowInfos(context.dataFlowInfo, context.dataFlowInfo);
    }

    /*
     * (a: SubjectType) is Type
     */
    private static void checkTypeCompatibility(
            @NotNull ExpressionTypingContext context,
            @Nullable JetType type,
            @NotNull JetType subjectType,
            @NotNull JetElement reportErrorOn
    ) {
        // TODO : Take auto casts into account?
        if (type == null) {
            return;
        }
        if (TypeUtils.isIntersectionEmpty(type, subjectType)) {
            context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType));
            return;
        }

        // check if the pattern is essentially a 'null' expression
        if (type == JetStandardClasses.getNullableNothingType() && !subjectType.isNullable()) {
            context.trace.report(SENSELESS_NULL_IN_WHEN.on(reportErrorOn));
        }
    }
}
