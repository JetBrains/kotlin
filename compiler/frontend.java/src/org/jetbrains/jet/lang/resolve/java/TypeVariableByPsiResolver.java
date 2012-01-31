package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiTypeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

/**
 * @author Stepan Koltsov
 *
 * @see TypeVariableByNameResolver
 */
public interface TypeVariableByPsiResolver {
    @NotNull
    TypeParameterDescriptor getTypeVariable(@NotNull PsiTypeParameter psiTypeParameter);
}
