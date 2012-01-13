package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public class JetPropertyAnnotation extends JetMethodOrPropertyAnnotation {
    protected JetPropertyAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }
    
    @NotNull
    public static JetPropertyAnnotation get(@NotNull PsiMethod psiMethod) {
        return new JetPropertyAnnotation(psiMethod.getModifierList().findAnnotation(JvmStdlibNames.JET_PROPERTY.getFqName()));
    }
}
