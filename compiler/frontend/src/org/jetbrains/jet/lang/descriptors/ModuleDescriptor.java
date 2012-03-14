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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;

/**
 * @author abreslav
 */
public class ModuleDescriptor extends DeclarationDescriptorImpl implements ClassOrNamespaceDescriptor, NamespaceDescriptorParent, NamespaceLikeBuilder {
    private NamespaceDescriptor rootNs;

    public ModuleDescriptor(String name) {
        super(null, Collections.<AnnotationDescriptor>emptyList(), name);
    }

    public void setRootNs(@NotNull NamespaceDescriptor rootNs) {
        if (this.rootNs != null) {
            throw new IllegalStateException();
        }
        this.rootNs = rootNs;
    }

    public NamespaceDescriptorImpl getRootNs() {
        return (NamespaceDescriptorImpl) rootNs;
    }

    @NotNull
    @Override
    public ModuleDescriptor substitute(TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitModuleDeclaration(this, data);
    }


    @NotNull
    @Override
    public DeclarationDescriptor getOwnerForChildren() {
        return this;
    }

    @Override
    public NamespaceDescriptorImpl getNamespace(String name) {
        throw new IllegalStateException();
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        if (namespaceDescriptor.getContainingDeclaration() != this) {
            throw new IllegalStateException();
        }
        setRootNs(namespaceDescriptor);
    }

    @Override
    public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
        throw new IllegalStateException();
    }

    @Override
    public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {
        throw new IllegalStateException();
    }

    @Override
    public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
        throw new IllegalStateException();
    }

    @Override
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
        throw new IllegalStateException();
    }

    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
        throw new IllegalStateException();
    }
}
