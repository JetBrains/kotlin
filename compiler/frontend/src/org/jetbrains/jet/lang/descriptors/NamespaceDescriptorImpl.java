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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.List;

/**
 * @author abreslav
 */
public class NamespaceDescriptorImpl extends AbstractNamespaceDescriptorImpl implements NamespaceLike {

    private WritableScope memberScope;

    public NamespaceDescriptorImpl(@Nullable DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull String name) {
        super(containingDeclaration, annotations, name);
    }

    public void initialize(@NotNull WritableScope memberScope) {
        this.memberScope = memberScope;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOwnerForChildren() {
        return this;
    }

    @Override
    @NotNull
    public WritableScope getMemberScope() {
        return memberScope;
    }

    @Override
    public NamespaceDescriptorImpl getNamespace(String name) {
        return (NamespaceDescriptorImpl) memberScope.getDeclaredNamespace(name);
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        memberScope.addNamespace(namespaceDescriptor);
    }

    @Override
    public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
        memberScope.addClassifierDescriptor(classDescriptor);
    }

    @Override
    public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {
        memberScope.addObjectDescriptor(objectDescriptor);
    }

    @Override
    public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
        memberScope.addFunctionDescriptor(functionDescriptor);
    }

    @Override
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
        memberScope.addPropertyDescriptor(propertyDescriptor);
    }

    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
        throw new IllegalStateException("Must be guaranteed not to happen by the parser");
    }
}
