package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Stepan Koltsov
 */
public class PsiFieldWrapper extends PsiMemberWrapper {
    public PsiFieldWrapper(@NotNull PsiMember psiMember) {
        super(psiMember);
    }
    
    public PsiField getPsiField() {
        return (PsiField) psiMember;
    }
    
    public PsiType getType() {
        return getPsiField().getType();
    }
}
