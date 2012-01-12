package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.kt.JetTypeParameterAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.JetValueParameterAnnotation;

/**
 * @author Stepan Koltsov
 */
public class PsiParameterWrapper {
    private final PsiParameter psiParameter;

    public PsiParameterWrapper(@NotNull PsiParameter psiParameter) {
        this.psiParameter = psiParameter;

        this.jetValueParameter = JetValueParameterAnnotation.get(psiParameter);
        this.jetTypeParameter = JetTypeParameterAnnotation.get(psiParameter);
    }

    private JetValueParameterAnnotation jetValueParameter;
    private JetTypeParameterAnnotation jetTypeParameter;

    @NotNull
    public PsiParameter getPsiParameter() {
        return psiParameter;
    }

    @NotNull
    public JetValueParameterAnnotation getJetValueParameter() {
        return jetValueParameter;
    }

    @NotNull
    public JetTypeParameterAnnotation getJetTypeParameter() {
        return jetTypeParameter;
    }
}
