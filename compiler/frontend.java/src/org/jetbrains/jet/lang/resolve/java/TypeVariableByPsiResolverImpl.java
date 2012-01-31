package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiTypeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class TypeVariableByPsiResolverImpl implements TypeVariableByPsiResolver {

    @NotNull
    private final List<JavaDescriptorResolver.TypeParameterDescriptorInitialization> typeParameters;
    @Nullable
    private final TypeVariableByPsiResolver parent;

    public TypeVariableByPsiResolverImpl(@NotNull List<JavaDescriptorResolver.TypeParameterDescriptorInitialization> typeParameters, TypeVariableByPsiResolver parent) {
        this.typeParameters = typeParameters;
        this.parent = parent;
    }

    @NotNull
    @Override
    public TypeParameterDescriptor getTypeVariable(@NotNull PsiTypeParameter psiTypeParameter) {
        for (JavaDescriptorResolver.TypeParameterDescriptorInitialization typeParameter : typeParameters) {
            if (typeParameter.psiTypeParameter == psiTypeParameter) {
                return typeParameter.descriptor;
            }
        }
        if (parent != null) {
            return parent.getTypeVariable(psiTypeParameter);
        }
        throw new RuntimeException("type parameter not found by PsiTypeParameter " + psiTypeParameter.getName()); // TODO report properly
    }

}
