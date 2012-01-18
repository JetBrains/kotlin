package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Stepan Koltsov
 */
class TypeSource {

    @NotNull
    private final String typeString;
    @NotNull
    private final PsiType psiType;

    TypeSource(@NotNull String typeString, @NotNull PsiType psiType) {
        this.typeString = typeString;
        this.psiType = psiType;
    }

    @NotNull
    public String getTypeString() {
        return typeString;
    }

    @NotNull
    public PsiType getPsiType() {
        return psiType;
    }
}
