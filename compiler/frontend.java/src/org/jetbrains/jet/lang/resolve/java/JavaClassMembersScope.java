/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.*;

/**
 * Class static of instance members.
 *
 * @author abreslav
 */
public class JavaClassMembersScope extends JavaClassOrPackageScope {
    @NotNull
    protected final PsiClass psiClass;
    private final boolean staticMembers;
    private final Map<String, ClassifierDescriptor> classifiers = Maps.newHashMap();
    private Collection<DeclarationDescriptor> allDescriptors;

    public JavaClassMembersScope(
            @NotNull ClassOrNamespaceDescriptor classOrNamespaceDescriptor,
            @NotNull PsiClass psiClass,
            @NotNull JavaSemanticServices semanticServices,
            boolean staticMembers) {
        super(classOrNamespaceDescriptor, semanticServices, psiClass);
        this.psiClass = psiClass;
        this.staticMembers = staticMembers;
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
    public Set<ClassDescriptor> getObjectDescriptors() {
        return Collections.emptySet();
    }

    /**
     * @see JavaPackageScope#getAllDescriptors()
     */
    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();

            allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveMethods(psiClass, descriptor));

            allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveFieldGroup(descriptor, psiClass, staticMembers));

            allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveInnerClasses(descriptor, psiClass, staticMembers));
        }
        return allDescriptors;
    }

    private ClassifierDescriptor doGetClassifierDescriptor(String name) {
        // TODO : suboptimal, walk the list only once
        for (PsiClass innerClass : psiClass.getAllInnerClasses()) {
            if (name.equals(innerClass.getName())) {
                if (innerClass.hasModifierProperty(PsiModifier.STATIC) != staticMembers) return null;
                ClassDescriptor classDescriptor = semanticServices.getDescriptorResolver().resolveClass(innerClass, DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
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
