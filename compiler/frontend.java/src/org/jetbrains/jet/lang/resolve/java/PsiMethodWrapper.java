package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.kt.JetConstructorAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.JetMethodAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class PsiMethodWrapper extends PsiMemberWrapper {

    public PsiMethodWrapper(@NotNull PsiMethod psiMethod) {
        super(psiMethod);
    }
    
    private List<PsiParameterWrapper> parameters;
    @NotNull
    public List<PsiParameterWrapper> getParameters() {
        if (parameters == null) {
            PsiParameter[] psiParameters = getPsiMethod().getParameterList().getParameters();
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

    public boolean isFinal() {
        return psiMember.hasModifierProperty(PsiModifier.FINAL);
    }

    private JetMethodAnnotation jetMethod;
    @NotNull
    public JetMethodAnnotation getJetMethod() {
        if (jetMethod == null) {
            jetMethod = JetMethodAnnotation.get(getPsiMethod());
        }
        return jetMethod;
    }

    private JetConstructorAnnotation jetConstructor;
    @NotNull
    public JetConstructorAnnotation getJetConstructor() {
        if (jetConstructor == null) {
            jetConstructor = JetConstructorAnnotation.get(getPsiMethod());
        }
        return jetConstructor;
    }

    @NotNull
    public PsiMethod getPsiMethod() {
        return (PsiMethod) psiMember;
    }
}
