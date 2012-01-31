package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;

import java.util.Collection;

/**
 * @author abreslav
 */
public class JavaPackageScope extends JavaClassOrPackageScope {
    private final String packageFQN;

    private Collection<DeclarationDescriptor> allDescriptors;

    public JavaPackageScope(
            @NotNull String packageFQN,
            @NotNull NamespaceDescriptor containingDescriptor,
            @NotNull JavaSemanticServices semanticServices) {
        super(containingDescriptor, semanticServices);
        this.packageFQN = packageFQN;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return semanticServices.getDescriptorResolver().resolveClass(getQualifiedName(name));
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull String name) {
        // TODO
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return semanticServices.getDescriptorResolver().resolveNamespace(getQualifiedName(name));
    }

    @Override
    protected PsiClassWrapper psiClass() {
        // If this package is actually a Kotlin namespace, then we access it through a namespace descriptor, and
        // Kotlin functions are already there
        NamespaceDescriptor kotlinNamespaceDescriptor = semanticServices.getKotlinNamespaceDescriptor(packageFQN);
        if (kotlinNamespaceDescriptor != null) {
            return null;
        }

        // TODO: what is GlobalSearchScope
        PsiClass psiClass = semanticServices.getDescriptorResolver().javaFacade.findClass(getQualifiedName(JvmAbi.PACKAGE_CLASS));
        if (psiClass == null) {
            return null;
        }

        return new PsiClassWrapper(psiClass);
    }

    @Override
    protected boolean staticMembers() {
        return true;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();

            final PsiPackage javaPackage = semanticServices.getDescriptorResolver().findPackage(packageFQN);

            if (javaPackage != null) {
                boolean isKotlinNamespace = semanticServices.getKotlinNamespaceDescriptor(javaPackage.getQualifiedName()) != null;
                final JavaDescriptorResolver descriptorResolver = semanticServices.getDescriptorResolver();

                for (PsiPackage psiSubPackage : javaPackage.getSubPackages()) {
                    allDescriptors.add(descriptorResolver.resolveNamespace(psiSubPackage.getQualifiedName()));
                }

                for (PsiClass psiClass : javaPackage.getClasses()) {
                    if (isKotlinNamespace && JvmAbi.PACKAGE_CLASS.equals(psiClass.getName())) continue;

                    // If this is a Kotlin class, we have already taken it through a containing namespace descriptor
                    ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(psiClass.getQualifiedName());
                    if (kotlinClassDescriptor != null) {
                        continue;
                    }

                    if (psiClass.hasModifierProperty(PsiModifier.PUBLIC)) {
                        ClassDescriptor classDescriptor = descriptorResolver.resolveClass(psiClass);
                        if (classDescriptor != null) {
                            allDescriptors.add(classDescriptor);
                        }
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
