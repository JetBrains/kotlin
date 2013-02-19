/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;

public class ModuleDescriptor extends DeclarationDescriptorImpl implements ClassOrNamespaceDescriptor, NamespaceDescriptorParent {
    private NamespaceDescriptor rootNamepsace;

    public ModuleDescriptor(@NotNull Name name) {
        super(Collections.<AnnotationDescriptor>emptyList(), name);
        if (!name.isSpecial()) {
            throw new IllegalArgumentException("module name must be special: " + name);
        }
    }

    public void setRootNamespace(@NotNull NamespaceDescriptor rootNs) {
        if (this.rootNamepsace != null) {
            throw new IllegalStateException("setRootNamespace() is called twice");
        }
        this.rootNamepsace = rootNs;
    }

    @Override
    @Nullable
    public DeclarationDescriptor getContainingDeclaration() {
        return null;
    }

    public NamespaceDescriptor getRootNamespace() {
        return rootNamepsace;
    }

    public NamespaceDescriptorImpl getRootNamespaceDescriptorImpl() {
        return (NamespaceDescriptorImpl) rootNamepsace;
    }

    @NotNull
    @Override
    public ModuleDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitModuleDeclaration(this, data);
    }


    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        if (namespaceDescriptor.getContainingDeclaration() != this) {
            throw new IllegalStateException();
        }
        setRootNamespace(namespaceDescriptor);
    }

}
