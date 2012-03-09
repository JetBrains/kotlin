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

import com.intellij.openapi.util.Ref;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.AUTOCAST;

/**
 * @author abreslav
 */
public class DataFlowUtils {
    private DataFlowUtils() {
    }

    @NotNull
    public static DataFlowInfo extractDataFlowInfoFromCondition(@Nullable JetExpression condition, final boolean conditionValue, @Nullable final WritableScope scopeToExtend, final ExpressionTypingContext context) {
        if (condition == null) return context.dataFlowInfo;
        final Ref<DataFlowInfo> result = new Ref<DataFlowInfo>(context.dataFlowInfo);
        condition.accept(new JetVisitorVoid() {
            @Override
            public void visitIsExpression(JetIsExpression expression) {
                if (conditionValue && !expression.isNegated() || !conditionValue && expression.isNegated()) {
                    JetPattern pattern = expression.getPattern();
                    result.set(context.patternsToDataFlowInfo.get(pattern));
                    if (scopeToExtend != null) {
                        List<VariableDescriptor> descriptors = context.patternsToBoundVariableLists.get(pattern);
                        if (descriptors != null) {
                            for (VariableDescriptor variableDescriptor : descriptors) {
                                scopeToExtend.addVariableDescriptor(variableDescriptor);
                            }
                        }
                    }
                }
            }

            @Override
            public void visitBinaryExpression(JetBinaryExpression expression) {
                IElementType operationToken = expression.getOperationToken();
                if (operationToken == JetTokens.ANDAND || operationToken == JetTokens.OROR) {
                    WritableScope actualScopeToExtend;
                    if (operationToken == JetTokens.ANDAND) {
                        actualScopeToExtend = conditionValue ? scopeToExtend : null;
                    }
                    else {
                        actualScopeToExtend = conditionValue ? null : scopeToExtend;
                    }

                    DataFlowInfo dataFlowInfo = extractDataFlowInfoFromCondition(expression.getLeft(), conditionValue, actualScopeToExtend, context);
                    JetExpression expressionRight = expression.getRight();
                    if (expressionRight != null) {
                        DataFlowInfo rightInfo = extractDataFlowInfoFromCondition(expressionRight, conditionValue, actualScopeToExtend, context);
                        DataFlowInfo.CompositionOperator operator;
                        if (operationToken == JetTokens.ANDAND) {
                            operator = conditionValue ? DataFlowInfo.AND : DataFlowInfo.OR;
                        }
                        else {
                            operator = conditionValue ? DataFlowInfo.OR : DataFlowInfo.AND;
                        }
                        dataFlowInfo = operator.compose(dataFlowInfo, rightInfo);
                    }
                    result.set(dataFlowInfo);
                }
                else  {
                    JetExpression left = expression.getLeft();
                    JetExpression right = expression.getRight();
                    if (right == null) return;

                    JetType lhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, left);
                    if (lhsType == null) return;
                    JetType rhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, right);
                    if (rhsType == null) return;

                    BindingContext bindingContext = context.trace.getBindingContext();
                    DataFlowValue leftValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(left, lhsType, bindingContext);
                    DataFlowValue rightValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(right, rhsType, bindingContext);

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
            public void visitUnaryExpression(JetUnaryExpression expression) {
                IElementType operationTokenType = expression.getOperationReference().getReferencedNameElementType();
                if (operationTokenType == JetTokens.EXCL) {
                    JetExpression baseExpression = expression.getBaseExpression();
                    if (baseExpression != null) {
                        result.set(extractDataFlowInfoFromCondition(baseExpression, !conditionValue, scopeToExtend, context));
                    }
                }
            }

            @Override
            public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
                JetExpression body = expression.getExpression();
                if (body != null) {
                    body.accept(this);
                }
            }
        });
        if (result.get() == null) {
            return context.dataFlowInfo;
        }
        return result.get();
    }

    @Nullable
    public static JetType checkType(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ExpressionTypingContext context) {
        if (expressionType == null || context.expectedType == null || context.expectedType == TypeUtils.NO_EXPECTED_TYPE ||
            JetTypeChecker.INSTANCE.isSubtypeOf(expressionType, context.expectedType)) {
            return expressionType;
        }

        DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(expression, expressionType, context.trace.getBindingContext());
        for (JetType possibleType : context.dataFlowInfo.getPossibleTypes(dataFlowValue)) {
            if (JetTypeChecker.INSTANCE.isSubtypeOf(possibleType, context.expectedType)) {
                if (dataFlowValue.isStableIdentifier()) {
                    context.trace.record(AUTOCAST, expression, possibleType);
                }
                else {
                    context.trace.report(AUTOCAST_IMPOSSIBLE.on(expression, possibleType, expression.getText()));
                }
                return possibleType;
            }
        }
        context.trace.report(TYPE_MISMATCH.on(expression, context.expectedType, expressionType));
        return expressionType;
    }

    @Nullable
    public static JetType checkStatementType(@NotNull JetExpression expression, @NotNull ExpressionTypingContext context) {
        if (context.expectedType != TypeUtils.NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(context.expectedType)) {
            context.trace.report(EXPECTED_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return JetStandardClasses.getUnitType();
    }

    @Nullable
    public static JetType checkImplicitCast(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ExpressionTypingContext context, boolean isStatement) {
        if (expressionType != null && context.expectedType == TypeUtils.NO_EXPECTED_TYPE && !isStatement &&
            (JetStandardClasses.isUnit(expressionType) || JetStandardClasses.isAny(expressionType))) {
            context.trace.report(IMPLICIT_CAST_TO_UNIT_OR_ANY.on(expression, expressionType));

        }
        return expressionType;
    }

    @Nullable
    public static JetType illegalStatementType(@NotNull JetExpression expression, @NotNull ExpressionTypingContext context, @NotNull ExpressionTypingInternals facade) {
        facade.checkStatementType(expression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
        context.trace.report(EXPRESSION_EXPECTED.on(expression, expression));
        return null;
    }
}
