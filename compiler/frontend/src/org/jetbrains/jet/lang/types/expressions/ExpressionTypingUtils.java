package org.jetbrains.jet.lang.types.expressions;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.calls.AutoCastUtils;
import org.jetbrains.jet.lang.resolve.calls.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.RESULT_TYPE_MISMATCH;
import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_MISMATCH;
import static org.jetbrains.jet.lang.resolve.BindingContext.MUST_BE_WRAPPED_IN_A_REF;

/**
 * @author abreslav
 */
public class ExpressionTypingUtils {

    @Nullable
    protected static ExpressionReceiver getExpressionReceiver(@NotNull ExpressionTypingFacade facade, @NotNull JetExpression expression, ExpressionTypingContext context) {
        JetType type = facade.getType(expression, context);
        if (type == null) {
            return null;
        }
        return new ExpressionReceiver(expression, type);
    }

    @NotNull
    protected static ExpressionReceiver safeGetExpressionReceiver(@NotNull ExpressionTypingFacade facade, @NotNull JetExpression expression, ExpressionTypingContext context) {
        return new ExpressionReceiver(expression, facade.safeGetType(expression, context));
    }

    @NotNull
    public static WritableScopeImpl newWritableScopeImpl(ExpressionTypingContext context) {
        return new WritableScopeImpl(context.scope, context.scope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(context.trace));
    }

    public static boolean isBoolean(@NotNull JetSemanticServices semanticServices, @NotNull JetType type) {
        return semanticServices.getTypeChecker().isConvertibleTo(type, semanticServices.getStandardLibrary().getBooleanType());
    }

    public static boolean ensureBooleanResult(JetExpression operationSign, String name, JetType resultType, ExpressionTypingContext context) {
        return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
    }

    public static boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName, ExpressionTypingContext context) {
        if (resultType != null) {
            // TODO : Relax?
            if (!isBoolean(context.semanticServices, resultType)) {
//                    context.trace.getErrorHandler().genericError(operationSign.getNode(), subjectName + " must return Boolean but returns " + resultType);
                context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, subjectName, context.semanticServices.getStandardLibrary().getBooleanType(), resultType));
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static JetType getDefaultType(JetSemanticServices semanticServices, IElementType constantType) {
        if (constantType == JetNodeTypes.INTEGER_CONSTANT) {
            return semanticServices.getStandardLibrary().getIntType();
        }
        else if (constantType == JetNodeTypes.FLOAT_CONSTANT) {
            return semanticServices.getStandardLibrary().getDoubleType();
        }
        else if (constantType == JetNodeTypes.BOOLEAN_CONSTANT) {
            return semanticServices.getStandardLibrary().getBooleanType();
        }
        else if (constantType == JetNodeTypes.CHARACTER_CONSTANT) {
            return semanticServices.getStandardLibrary().getCharType();
        }
        else if (constantType == JetNodeTypes.RAW_STRING_CONSTANT) {
            return semanticServices.getStandardLibrary().getStringType();
        }
        else if (constantType == JetNodeTypes.NULL) {
            return JetStandardClasses.getNullableNothingType();
        }
        else {
            throw new IllegalArgumentException("Unsupported constant type: " + constantType);
        }
    }

    @NotNull
    public static DataFlowInfo extractDataFlowInfoFromCondition(@Nullable JetExpression condition, final boolean conditionValue, @Nullable final WritableScope scopeToExtend, final ExpressionTypingContext context) {
        if (condition == null) return context.dataFlowInfo;
        final DataFlowInfo[] result = new DataFlowInfo[] {context.dataFlowInfo};
        condition.accept(new JetVisitorVoid() {
            @Override
            public void visitIsExpression(JetIsExpression expression) {
                if (conditionValue && !expression.isNegated() || !conditionValue && expression.isNegated()) {
                    JetPattern pattern = expression.getPattern();
                    result[0] = context.patternsToDataFlowInfo.get(pattern);
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
                    result[0] = dataFlowInfo;
                }
                else if (operationToken == JetTokens.EQEQ
                         || operationToken == JetTokens.EXCLEQ
                         || operationToken == JetTokens.EQEQEQ
                         || operationToken == JetTokens.EXCLEQEQEQ) {
                    JetExpression left = expression.getLeft();
                    JetExpression right = expression.getRight();
                    if (right == null) return;

                    if (!(left instanceof JetSimpleNameExpression)) {
                        JetExpression tmp = left;
                        left = right;
                        right = tmp;

                        if (!(left instanceof JetSimpleNameExpression)) {
                            return;
                        }
                    }

                    VariableDescriptor variableDescriptor = AutoCastUtils.getVariableDescriptorFromSimpleName(context.trace.getBindingContext(), left);
                    if (variableDescriptor == null) return;

                    // TODO : validate that DF makes sense for this variable: local, val, internal w/backing field, etc

                    // Comparison to a non-null expression
                    JetType rhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, right);
                    if (rhsType != null && !rhsType.isNullable()) {
                        extendDataFlowWithNullComparison(operationToken, variableDescriptor, !conditionValue);
                        return;
                    }

                    VariableDescriptor rightVariable = AutoCastUtils.getVariableDescriptorFromSimpleName(context.trace.getBindingContext(), right);
                    if (rightVariable != null) {
                        JetType lhsType = context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, left);
                        if (lhsType != null && !lhsType.isNullable()) {
                            extendDataFlowWithNullComparison(operationToken, rightVariable, !conditionValue);
                            return;
                        }
                    }

                    // Comparison to 'null'
                    if (!(right instanceof JetConstantExpression)) {
                        return;
                    }
                    JetConstantExpression constantExpression = (JetConstantExpression) right;
                    if (constantExpression.getNode().getElementType() != JetNodeTypes.NULL) {
                        return;
                    }

                    extendDataFlowWithNullComparison(operationToken, variableDescriptor, conditionValue);
                }
            }

            private void extendDataFlowWithNullComparison(IElementType operationToken, @NotNull VariableDescriptor variableDescriptor, boolean equalsToNull) {
                if (operationToken == JetTokens.EQEQ || operationToken == JetTokens.EQEQEQ) {
                    result[0] = context.dataFlowInfo.equalsToNull(variableDescriptor, !equalsToNull);
                }
                else if (operationToken == JetTokens.EXCLEQ || operationToken == JetTokens.EXCLEQEQEQ) {
                    result[0] = context.dataFlowInfo.equalsToNull(variableDescriptor, equalsToNull);
                }
            }

            @Override
            public void visitUnaryExpression(JetUnaryExpression expression) {
                IElementType operationTokenType = expression.getOperationSign().getReferencedNameElementType();
                if (operationTokenType == JetTokens.EXCL) {
                    JetExpression baseExpression = expression.getBaseExpression();
                    if (baseExpression != null) {
                        result[0] = extractDataFlowInfoFromCondition(baseExpression, !conditionValue, scopeToExtend, context);
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
        if (result[0] == null) {
            return context.dataFlowInfo;
        }
        return result[0];
    }

    public static boolean isTypeFlexible(@Nullable JetExpression expression) {
        if (expression == null) return false;

        return TokenSet.create(
                JetNodeTypes.INTEGER_CONSTANT,
                JetNodeTypes.FLOAT_CONSTANT
        ).contains(expression.getNode().getElementType());
    }

    @Nullable
    public static JetType checkType(@Nullable JetType expressionType, @NotNull JetExpression expression, @NotNull ExpressionTypingContext context) {
        if (expressionType == null || context.expectedType == null || context.expectedType == TypeUtils.NO_EXPECTED_TYPE ||
            context.semanticServices.getTypeChecker().isSubtypeOf(expressionType, context.expectedType)) {
            return expressionType;
        }
        if (AutoCastUtils.castExpression(expression, context.expectedType, context.dataFlowInfo, context.trace) == null) {
            context.trace.report(TYPE_MISMATCH.on(expression, context.expectedType, expressionType));
            return expressionType;
        }
        return context.expectedType;
    }

    public static void checkWrappingInRef(JetExpression expression, ExpressionTypingContext context) {
        if (!(expression instanceof JetSimpleNameExpression)) return;
        JetSimpleNameExpression simpleName = (JetSimpleNameExpression) expression;
        VariableDescriptor variable = AutoCastUtils.getVariableDescriptorFromSimpleName(context.trace.getBindingContext(), simpleName);
        if (variable != null) {
            DeclarationDescriptor containingDeclaration = variable.getContainingDeclaration();
            if (context.scope.getContainingDeclaration() != containingDeclaration && containingDeclaration instanceof CallableDescriptor) {
                context.trace.record(MUST_BE_WRAPPED_IN_A_REF, variable);
            }
        }
    }
}
