package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface ValueArgumentPsi {

    class ArgumentExpressionWrapper implements ValueArgumentPsi {

        private final JetExpression expression;

        public ArgumentExpressionWrapper(JetExpression expression) {
            this.expression = expression;
        }

        @Override
        public PsiElement asElement() {
            return expression;
        }

        @Override
        public String getArgumentName() {
            return null;
        }

        @Override
        public boolean isNamed() {
            return false;
        }

        @Override
        public JetExpression getArgumentExpression() {
            return expression;
        }
    }

    PsiElement asElement();
    @Nullable
    String getArgumentName();

    boolean isNamed();

    @Nullable
    public JetExpression getArgumentExpression();
}
