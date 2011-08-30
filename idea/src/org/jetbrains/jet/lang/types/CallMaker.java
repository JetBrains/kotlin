package org.jetbrains.jet.lang.types;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class CallMaker {

    private static class ExpressionValueArgument implements ValueArgument {

        private final JetExpression expression;

        private final PsiElement reportErrorsOn;
        private ExpressionValueArgument(@NotNull JetExpression expression) {
            this(expression, expression);
        }

        private ExpressionValueArgument(@Nullable JetExpression expression, @NotNull PsiElement reportErrorsOn) {
            this.expression = expression;
            this.reportErrorsOn = expression == null ? reportErrorsOn : expression;
        }

        @Override
        public JetExpression getArgumentExpression() {
            return expression;
        }

        @Override
        public JetValueArgumentName getArgumentName() {
            return null;
        }

        @Override
        public boolean isNamed() {
            return false;
        }

        @Override
        public boolean isOut() {
            return false;
        }

        @Override
        public boolean isRef() {
            return false;
        }

        @NotNull
        @Override
        public PsiElement asElement() {
            return reportErrorsOn;
        }

    }
    private static abstract class CallStub implements Call {

        @Override
        public JetValueArgumentList getValueArgumentList() {
            return null;
        }
        @NotNull
        @Override
        public List<JetExpression> getFunctionLiteralArguments() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public List<JetTypeProjection> getTypeArguments() {
            return Collections.emptyList();
        }

        @Override
        public JetTypeArgumentList getTypeArgumentList() {
            return null;
        }

    }
    public static Call makeCallWithArguments(final JetExpression calleeExpression, final List<? extends ValueArgument> valueArguments) {
        return new CallStub() {
            @Override
            public JetExpression getCalleeExpression() {
                return calleeExpression;
            }

            @NotNull
            @Override
            public List<? extends ValueArgument> getValueArguments() {
                return valueArguments;
            }
        };
    }

    public static Call makeCall(final JetExpression calleeExpression, final List<JetExpression> argumentExpressions) {
        List<ValueArgument> arguments = Lists.newArrayList();
        for (JetExpression argumentExpression : argumentExpressions) {
            arguments.add(makeValueArgument(argumentExpression, calleeExpression));
        }
        return makeCallWithArguments(calleeExpression, arguments);
    }

    public static Call makeCall(final JetBinaryExpression expression) {
        return makeCall(expression.getOperationReference(), Collections.singletonList(expression.getRight()));
    }

    public static Call makeCall(final JetUnaryExpression expression) {
        return makeCallWithArguments(expression.getOperationSign(), Collections.<ValueArgument>emptyList());
    }

    public static Call makeCall(final JetArrayAccessExpression arrayAccessExpression, final JetExpression rightHandSide) {
        List<JetExpression> arguments = Lists.newArrayList(arrayAccessExpression.getIndexExpressions());
        arguments.add(rightHandSide);
        return makeCall(arrayAccessExpression, arguments);
    }

    public static ValueArgument makeValueArgument(@NotNull JetExpression expression) {
        return makeValueArgument(expression, expression);
    }

    public static ValueArgument makeValueArgument(@Nullable JetExpression expression, @NotNull PsiElement reportErrorsOn) {
        return new ExpressionValueArgument(expression, reportErrorsOn);
    }

    public static Call makePropertyCall(@NotNull JetSimpleNameExpression nameExpression) {
        return makeCall(nameExpression, Collections.<JetExpression>emptyList());
    }
}
