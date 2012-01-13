package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Stepan Koltsov
 */
public class PsiAnnotationUtils {

    @NotNull
    public static String getStringAttribute(@Nullable PsiAnnotation annotation, @NotNull String field, @NotNull String defaultValue) {
        return getAttribute(annotation, field, defaultValue);
    }

    public static boolean getBooleanAttribute(@Nullable PsiAnnotation annotation, @NotNull String field, boolean defaultValue) {
        return getAttribute(annotation, field, defaultValue);
    }

    private static <T> T getAttribute(@Nullable PsiAnnotation annotation, @NotNull String field, @NotNull T defaultValue) {
        if (annotation == null) {
            return defaultValue;
        } else {
            PsiLiteralExpression attributeValue = (PsiLiteralExpression) annotation.findAttributeValue(field);
            if (attributeValue != null) {
                return (T) attributeValue.getValue();
            } else {
                return defaultValue;
            }
        }
    }

}
