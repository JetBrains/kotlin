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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstantChecker;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.constants.evaluate.EvaluatePackage;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.*;

public class DataFlowUtils {
    private DataFlowUtils() {
    }

    @NotNull
    public static DataFlowInfo extractDataFlowInfoFromCondition(@Nullable JetExpression condition, final boolean conditionValue, final ExpressionTypingContext context) {
        if (condition == null) return context.dataFlowInfo;
        final Ref<DataFlowInfo> result = new Ref<DataFlowInfo>(null);
        condition.accept(new JetVisitorVoid() {
            @Override
            public void visitIsExpression(@NotNull JetIsExpression expression) {
                if (conditionValue && !expression.isNegated() || !conditionValue && expression.isNegated()) {
                    result.set(context.trace.get(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression));
                }
            }

            @Override
            public void visitBinaryExpression(@NotNull JetBinaryExpression expression) {
                IElementType operationToken = expression.getOperationToken();
                if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationToken)) {
                    DataFlowInfo dataFlowInfo = extractDataFlowInfoFromCondition(expression.getLeft(), conditionValue, context);
                    JetExpression expressionRight = expression.getRight();
                    if (expressionRight != null) {
                        DataFlowInfo rightInfo = extractDataFlowInfoFromCondition(expressionRight, conditionValue, context);
                        boolean and = operationToken == JetTokens.ANDAND;
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
                    JetExpression left = expression.getLeft();
                    if (left == null) return;
                    JetExpression right = expression.getRight();
                    if (right == null) return;

                    JetType lhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, left);
                    if (lhsType == null) return;
                    JetType rhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, right);
                    if (rhsType == null) return;

                    BindingContext bindingContext = context.trace.getBindingContext();
                    DataFlowValue leftValue = DataFlowValueFactory.createDataFlowValue(left, lhsType, bindingContext);
                    DataFlowValue rightValue = DataFlowValueFactory.createDataFlowValue(right, rhsType, bindingContext);

                    Boolean equals = null;
                    if (operationToken == JetTokens.EQEQ || operationToken == JetTokens.EQEQEQ) {
                        equals = true;
                    }
                    else if (operationToken == JetTokens.EXCLEQ || operationToken == JetTokens.EXCLEQEQEQ) {
                        equals = false;
                    }
                    if (equals != null) {
                        if (equals == conditionValue) { // this means: equals && conditionValue || !equals && !conditionValue
                            result.set(context.dataFlowInfo.equate(leftValue, rightValue));
                        }
                        else {
                            result.set(context.dataFlowInfo.disequate(leftValue, rightValue));
                        }

                    }
                }
            }

            @Override
            public void visitUnaryExpression(@NotNull JetUnaryExpression expression) {
                IElementType operationTokenType = expression.getOperationReference().getReferencedNameElementType();
                if (operationTokenType == JetTokens.EXCL) {
                    JetExpression baseExpression = expression.getBaseExpression();
                    if (baseExpression != null) {
                        result.set(extractDataFlowInfoFromCondition(baseExpression, !conditionValue, context));
                    }
                }
            }

            @Override
            public void visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression) {
                JetExpression body = expression.getExpression();
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

    @NotNull
    public static JetTypeInfo checkType(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ResolutionContext context, @NotNull DataFlowInfo dataFlowInfo) {
        return JetTypeInfo.create(checkType(expressionType, expression, context), dataFlowInfo);
    }

    @NotNull
    public static JetTypeInfo checkType(@NotNull JetTypeInfo typeInfo, @NotNull JetExpression expression, @NotNull ResolutionContext context) {
        JetType type = checkType(typeInfo.getType(), expression, context);
        if (type == typeInfo.getType()) {
            return typeInfo;
        }
        return JetTypeInfo.create(type, typeInfo.getDataFlowInfo());
    }

    @Nullable
    public static JetType checkType(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ResolutionContext context) {
        return checkType(expressionType, expression, context, (Ref<Boolean>) null);
    }

    @Nullable
    public static JetType checkType(
            @Nullable final JetType expressionType,
            @NotNull JetExpression expressionToCheck,
            @NotNull final ResolutionContext c,
            @Nullable Ref<Boolean> hasError
    ) {
        if (hasError != null) hasError.set(false);

        final JetExpression expression = JetPsiUtil.safeDeparenthesize(expressionToCheck, false);
        recordExpectedType(c.trace, expression, c.expectedType);

        if (expressionType == null) return null;

        if (noExpectedType(c.expectedType) || !c.expectedType.getConstructor().isDenotable() ||
            JetTypeChecker.DEFAULT.isSubtypeOf(expressionType, c.expectedType)) {

            if (!noExpectedType(c.expectedType)) {
                Approximation.Info approximationInfo = TypesPackage.getApproximationTo(expressionType, c.expectedType,
                        new Approximation.DataFlowExtras() {
                            private DataFlowValue getDataFlowValue() {
                                return DataFlowValueFactory.createDataFlowValue(expression, expressionType, c.trace.getBindingContext());
                            }

                            @Override
                            public boolean getCanBeNull() {
                                return c.dataFlowInfo.getNullability(getDataFlowValue()).canBeNull();
                            }

                            @Override
                            public Set<JetType> getPossibleTypes() {
                                return c.dataFlowInfo.getPossibleTypes(getDataFlowValue());
                            }

                            @NotNull
                            @Override
                            public String getPresentableText() {
                                return StringUtil.trimMiddle(expression.getText(), 50);
                            }
                        }
                );
                if (approximationInfo != null) {
                    c.trace.record(BindingContext.EXPRESSION_RESULT_APPROXIMATION, expression, approximationInfo);
                }
            }

            return expressionType;
        }

        if (expression instanceof JetConstantExpression) {
            CompileTimeConstant<?> value = ConstantExpressionEvaluator.OBJECT$.evaluate(expression, c.trace, c.expectedType);
            if (value instanceof IntegerValueTypeConstant) {
                value = EvaluatePackage.createCompileTimeConstantWithType((IntegerValueTypeConstant) value, c.expectedType);
            }
            boolean error = new CompileTimeConstantChecker(c.trace, true)
                    .checkConstantExpressionType(value, (JetConstantExpression) expression, c.expectedType);
            if (hasError != null) hasError.set(error);
            return expressionType;
        }

        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, expressionType, c.trace.getBindingContext());

        for (JetType possibleType : c.dataFlowInfo.getPossibleTypes(dataFlowValue)) {
            if (JetTypeChecker.DEFAULT.isSubtypeOf(possibleType, c.expectedType)) {
                SmartCastUtils.recordCastOrError(expression, possibleType, c.trace, dataFlowValue.isStableIdentifier(), false);
                return possibleType;
            }
        }
        c.trace.report(TYPE_MISMATCH.on(expression, c.expectedType, expressionType));
        if (hasError != null) hasError.set(true);
        return expressionType;
    }

    public static void recordExpectedType(@NotNull BindingTrace trace, @NotNull JetExpression expression, @NotNull JetType expectedType) {
        if (expectedType != NO_EXPECTED_TYPE) {
            JetType normalizeExpectedType = expectedType == UNIT_EXPECTED_TYPE ? KotlinBuiltIns.getInstance().getUnitType() : expectedType;
            trace.record(BindingContext.EXPECTED_EXPRESSION_TYPE, expression, normalizeExpectedType);
        }
    }

    @NotNull
    public static JetTypeInfo checkStatementType(@NotNull JetExpression expression, @NotNull ResolutionContext context, @NotNull DataFlowInfo dataFlowInfo) {
        return JetTypeInfo.create(checkStatementType(expression, context), dataFlowInfo);
    }

    @Nullable
    public static JetType checkStatementType(@NotNull JetExpression expression, @NotNull ResolutionContext context) {
        if (!noExpectedType(context.expectedType) && !KotlinBuiltIns.isUnit(context.expectedType) && !context.expectedType.isError()) {
            context.trace.report(EXPECTED_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return KotlinBuiltIns.getInstance().getUnitType();
    }

    @NotNull
    public static JetTypeInfo checkImplicitCast(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ExpressionTypingContext context, boolean isStatement, DataFlowInfo dataFlowInfo) {
        return JetTypeInfo.create(checkImplicitCast(expressionType, expression, context, isStatement), dataFlowInfo);
    }

    @Nullable
    public static JetType checkImplicitCast(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ExpressionTypingContext context, boolean isStatement) {
        if (expressionType != null && context.expectedType == NO_EXPECTED_TYPE && context.contextDependency == INDEPENDENT && !isStatement
                && (KotlinBuiltIns.isUnit(expressionType) || KotlinBuiltIns.isAnyOrNullableAny(expressionType))
                && !TypesPackage.isDynamic(expressionType)) {
            context.trace.report(IMPLICIT_CAST_TO_UNIT_OR_ANY.on(expression, expressionType));
        }
        return expressionType;
    }

    @NotNull
    public static JetTypeInfo illegalStatementType(@NotNull JetExpression expression, @NotNull ExpressionTypingContext context, @NotNull ExpressionTypingInternals facade) {
        facade.checkStatementType(
                expression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT));
        context.trace.report(EXPRESSION_EXPECTED.on(expression, expression));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @NotNull
    public static Collection<JetType> getAllPossibleTypes(
            @NotNull JetExpression expression,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType type,
            @NotNull BindingContext bindingContext
    ) {
        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, type, bindingContext);
        Collection<JetType> possibleTypes = Sets.newHashSet(type);
        if (dataFlowValue.isStableIdentifier()) {
            possibleTypes.addAll(dataFlowInfo.getPossibleTypes(dataFlowValue));
        }
        return possibleTypes;
    }
}
