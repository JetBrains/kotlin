package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;

import java.util.Collections;
import java.util.Set;

/**
 * @author abreslav
 */
public class JavaPackageScope extends JetScopeImpl {
    private final JavaSemanticServices semanticServices;
    private final DeclarationDescriptor containingDescriptor;
    private final String packagePrefix;

    public JavaPackageScope(@NotNull String packageFQN, DeclarationDescriptor containingDescriptor, JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
        this.packagePrefix = packageFQN.isEmpty() ? "" : packageFQN + ".";
        this.containingDescriptor = containingDescriptor;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return semanticServices.getDescriptorResolver().resolveClass(getQualifiedName(name));
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return semanticServices.getDescriptorResolver().resolveNamespace(getQualifiedName(name));
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        // TODO: what is GlobalSearchScope
        PsiClass psiClass = semanticServices.getDescriptorResolver().javaFacade.findClass(packagePrefix + "namespace");
        if (psiClass == null) {
            return Collections.emptySet();
        }

        if (containingDescriptor == null) {
            return Collections.emptySet();
        }

        return semanticServices.getDescriptorResolver().resolveFunctionGroup(containingDescriptor, psiClass, null, name, true);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDescriptor;
    }

    private String getQualifiedName(String name) {
        return packagePrefix + name;
    }
}
