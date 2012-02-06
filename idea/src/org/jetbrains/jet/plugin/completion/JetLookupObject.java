package org.jetbrains.jet.plugin.completion;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for removing duplicates
 *
 * @author Nikolay Krasko
 */
public class JetLookupObject {
    @NotNull
    private final DeclarationDescriptor descriptor;

    @Nullable
    private final PsiElement psiElement;

    public JetLookupObject(@NotNull DeclarationDescriptor descriptor, @Nullable PsiElement psiElement) {
        this.descriptor = descriptor;
        this.psiElement = psiElement;
    }

    @NotNull
    public DeclarationDescriptor getDescriptor() {
        return descriptor;
    }

    @Nullable
    public PsiElement getPsiElement() {
        return psiElement;
    }
}
