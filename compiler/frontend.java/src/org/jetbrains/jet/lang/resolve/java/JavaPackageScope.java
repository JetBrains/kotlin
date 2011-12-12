package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author abreslav
 */
public class JavaPackageScope extends JetScopeImpl {
    private final JavaSemanticServices semanticServices;
    private final DeclarationDescriptor containingDescriptor;
    private final String packageFQN;

    private Collection<DeclarationDescriptor> allDescriptors;

    public JavaPackageScope(@NotNull String packageFQN, DeclarationDescriptor containingDescriptor, JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
        this.packageFQN = packageFQN;
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
        // If this package is actually a Kotlin namespace, then we access it through a namespace descriptor, and
        // Kotlin functions are already there
        NamespaceDescriptor kotlinNamespaceDescriptor = semanticServices.getKotlinNamespaceDescriptor(packageFQN);
        if (kotlinNamespaceDescriptor != null) {
            return Collections.emptySet();
        }

        // TODO: what is GlobalSearchScope
        PsiClass psiClass = semanticServices.getDescriptorResolver().javaFacade.findClass(getQualifiedName("namespace"));
        if (psiClass == null) {
            return Collections.emptySet();
        }

        if (containingDescriptor == null) {
            return Collections.emptySet();
        }

        return semanticServices.getDescriptorResolver().resolveFunctionGroup(containingDescriptor, psiClass, null, name, true);
//            return Collections.emptySet();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDescriptor;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();

            final PsiPackage javaPackage = semanticServices.getDescriptorResolver().findPackage(packageFQN);

            if (javaPackage != null) {
                final JavaDescriptorResolver descriptorResolver = semanticServices.getDescriptorResolver();

                for (PsiPackage psiSubPackage : javaPackage.getSubPackages()) {
                    allDescriptors.add(descriptorResolver.resolveNamespace(psiSubPackage.getQualifiedName()));
                }

                for (PsiClass psiClass : javaPackage.getClasses()) {
                    // If this is a Kotlin class, we have already taken it through a containing namespace descriptor
                    ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(psiClass.getQualifiedName());
                    if (kotlinClassDescriptor != null) {
                        continue;
                    }

                    if (psiClass.hasModifierProperty(PsiModifier.PUBLIC)) {
                        allDescriptors.add(descriptorResolver.resolveClass(psiClass));
                    }
                }
            }
        }

        return allDescriptors;
    }

    private String getQualifiedName(String name) {
        return (packageFQN.isEmpty() ? "" : packageFQN + ".") + name;
    }
}
