package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public class JavaClassMembersScope extends JavaClassOrPackageScope {
    private final PsiClassWrapper psiClass;
    private final boolean staticMembers;
    private final Map<String, ClassifierDescriptor> classifiers = Maps.newHashMap();
    private Collection<DeclarationDescriptor> allDescriptors;

    public JavaClassMembersScope(
            @NotNull DeclarationDescriptor classOrNamespaceDescriptor,
            @NotNull PsiClass psiClass,
            @NotNull JavaSemanticServices semanticServices,
            boolean staticMembers) {
        super(classOrNamespaceDescriptor, semanticServices);
        this.psiClass = new PsiClassWrapper(psiClass);
        this.staticMembers = staticMembers;
    }

    @Override
    protected PsiClassWrapper psiClass() {
        return psiClass;
    }

    @Override
    protected boolean staticMembers() {
        return staticMembers;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassifierDescriptor classifierDescriptor = classifiers.get(name);
        if (classifierDescriptor == null) {
            classifierDescriptor = doGetClassifierDescriptor(name);
            classifiers.put(name, classifierDescriptor);
        }
        return classifierDescriptor;
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull String name) {
        return null;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();
            TypeSubstitutor substitutorForGenericSupertypes = getTypeSubstitutorForSupertypes();

            allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveMethods(psiClass.getPsiClass(), descriptor, staticMembers, substitutorForGenericSupertypes));

            allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveFieldGroup(descriptor, psiClass.getPsiClass(), staticMembers));

            allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveInnerClasses(descriptor, psiClass.getPsiClass(), staticMembers));
        }
        return allDescriptors;
    }

    private TypeSubstitutor getTypeSubstitutorForSupertypes() {
        TypeSubstitutor substitutorForGenericSupertypes;
        if (descriptor instanceof ClassDescriptor) {
            substitutorForGenericSupertypes = semanticServices.getDescriptorResolver().createSubstitutorForGenericSupertypes((ClassDescriptor) descriptor);
        }
        else {
            substitutorForGenericSupertypes = TypeSubstitutor.EMPTY;
        }
        return substitutorForGenericSupertypes;
    }

    private ClassifierDescriptor doGetClassifierDescriptor(String name) {
        // TODO : suboptimal, walk the list only once
        for (PsiClass innerClass : psiClass.getPsiClass().getAllInnerClasses()) {
            if (name.equals(innerClass.getName())) {
                if (innerClass.hasModifierProperty(PsiModifier.STATIC) != staticMembers) return null;
                ClassDescriptor classDescriptor = semanticServices.getDescriptorResolver().resolveClass(innerClass);
                if (classDescriptor != null) {
                    return classDescriptor;
                }
            }
        }
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return null;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        return ReceiverDescriptor.NO_RECEIVER; // Should never occur, we don't sit in a Java class...
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        // we cannot really be scoped inside here
    }
}
