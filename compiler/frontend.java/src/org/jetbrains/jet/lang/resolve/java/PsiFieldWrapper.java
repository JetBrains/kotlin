package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;

/**
 * @author Stepan Koltsov
 */
public class PsiFieldWrapper extends PsiMemberWrapper {
    public PsiFieldWrapper(@NotNull PsiMember psiMember) {
        super(psiMember);
    }
}
