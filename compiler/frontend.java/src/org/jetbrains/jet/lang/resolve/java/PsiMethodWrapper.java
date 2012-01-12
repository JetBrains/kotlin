package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.kt.JetConstructorAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.JetMethodAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.JetPropertyAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class PsiMethodWrapper {
    @NotNull
    private final PsiMethod psiMethod;

    public PsiMethodWrapper(@NotNull PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
    }
    
    private List<PsiParameterWrapper> parameters;
    @NotNull
    public List<PsiParameterWrapper> getParameters() {
        if (parameters == null) {
            PsiParameter[] psiParameters = psiMethod.getParameterList().getParameters();
            parameters = new ArrayList<PsiParameterWrapper>(psiParameters.length);
            for (int i = 0; i < psiParameters.length; ++i) {
                parameters.add(new PsiParameterWrapper(psiParameters[i]));
            }
        }
        return parameters;
    }

    @NotNull
    public PsiParameterWrapper getParameter(int i) {
        return getParameters().get(i);
    }

    public boolean isStatic() {
        return psiMethod.getModifierList().hasExplicitModifier(PsiModifier.STATIC);
    }

    public boolean isPrivate() {
        return psiMethod.hasModifierProperty(PsiModifier.PRIVATE);
    }

    private JetMethodAnnotation jetMethod;
    @NotNull
    public JetMethodAnnotation getJetMethod() {
        if (jetMethod == null) {
            jetMethod = JetMethodAnnotation.get(psiMethod);
        }
        return jetMethod;
    }

    private JetConstructorAnnotation jetConstructor;
    @NotNull
    public JetConstructorAnnotation getJetConstructor() {
        if (jetConstructor == null) {
            jetConstructor = JetConstructorAnnotation.get(psiMethod);
        }
        return jetConstructor;
    }

    private JetPropertyAnnotation jetProperty;
    @NotNull
    public JetPropertyAnnotation getJetProperty() {
        if (jetProperty == null) {
            jetProperty = JetPropertyAnnotation.get(psiMethod);
        }
        return jetProperty;
    }
}
