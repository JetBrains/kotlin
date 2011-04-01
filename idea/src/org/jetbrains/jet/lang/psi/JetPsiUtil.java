package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public class JetPsiUtil {
    @Nullable
    public static JetExpression deparenthesize(@NotNull JetExpression expression) {
        JetExpression result = expression;
        while (result instanceof JetParenthesizedExpression) {
            result = ((JetParenthesizedExpression) expression).getExpression();
        }
        return result;
    }
}
