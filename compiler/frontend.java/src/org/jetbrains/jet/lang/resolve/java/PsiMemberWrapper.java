package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;

/**
 * @author Stepan Koltsov
 */
public class PsiMemberWrapper {

    @NotNull
    protected final PsiMember psiMember;

    public PsiMemberWrapper(@NotNull PsiMember psiMember) {
        this.psiMember = psiMember;
    }

    public boolean isStatic() {
        return psiMember.hasModifierProperty(PsiModifier.STATIC);
    }

    public boolean isPrivate() {
        return psiMember.hasModifierProperty(PsiModifier.PRIVATE);
    }

}
