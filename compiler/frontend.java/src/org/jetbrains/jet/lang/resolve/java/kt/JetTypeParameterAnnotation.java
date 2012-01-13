package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public class JetTypeParameterAnnotation extends PsiAnnotationWrapper {
    
    protected JetTypeParameterAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    @NotNull
    public static JetTypeParameterAnnotation get(@NotNull PsiParameter psiParameter) {
        return new JetTypeParameterAnnotation(psiParameter.getModifierList().findAnnotation(JvmStdlibNames.JET_TYPE_PARAMETER.getFqName()));
    }
}
