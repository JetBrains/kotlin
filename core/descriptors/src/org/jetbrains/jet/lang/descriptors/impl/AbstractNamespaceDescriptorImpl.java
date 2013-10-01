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

package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

public abstract class AbstractNamespaceDescriptorImpl extends DeclarationDescriptorNonRootImpl implements NamespaceDescriptor {
    public AbstractNamespaceDescriptorImpl(
            @NotNull NamespaceDescriptorParent containingDeclaration,
            List<AnnotationDescriptor> annotations,
            @NotNull Name name) {

        super(containingDeclaration, annotations, name);

        boolean rootAccordingToContainer = containingDeclaration instanceof ModuleDescriptor;
        if (rootAccordingToContainer != name.isSpecial()) {
            throw new IllegalStateException("something is wrong, name: " + name + ", container: " + containingDeclaration);
        }
    }

    @Override
    @NotNull
    public NamespaceDescriptorParent getContainingDeclaration() {
        return (NamespaceDescriptorParent) super.getContainingDeclaration();
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        throw new IllegalStateException("immutable");
    }

    @NotNull
    @Override
    public NamespaceDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException("This operation does not make sense for a namespace");
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitNamespaceDescriptor(this, data);
    }
}
