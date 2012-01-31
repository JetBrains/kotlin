package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;

import java.util.Collections;
import java.util.Set;

/**
 * @author Stepan Koltsov
 */
public abstract class JavaClassOrPackageScope extends JetScopeImpl {

    @NotNull
    protected final ClassOrNamespaceDescriptor descriptor;
    protected final JavaSemanticServices semanticServices;

    protected JavaClassOrPackageScope(@NotNull ClassOrNamespaceDescriptor descriptor, @NotNull JavaSemanticServices semanticServices) {
        this.descriptor = descriptor;
        this.semanticServices = semanticServices;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return descriptor;
    }

    @Nullable
    protected abstract PsiClassWrapper psiClass();

    protected abstract boolean staticMembers();

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull String name) {
        PsiClassWrapper psiClass = psiClass();

        if (psiClass == null) {
            return Collections.emptySet();
        }

        // TODO: cache
        return semanticServices.getDescriptorResolver().resolveFieldGroupByName(
                descriptor, psiClass.getPsiClass(), name, staticMembers());
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        PsiClassWrapper psiClassForPackage = psiClass();

        if (psiClassForPackage == null) {
            return Collections.emptySet();
        }

        return semanticServices.getDescriptorResolver().resolveFunctionGroup(descriptor, psiClassForPackage.getPsiClass(), name, staticMembers());
    }

}
