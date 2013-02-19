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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.List;

public class NamespaceDescriptorImpl extends AbstractNamespaceDescriptorImpl {

    private WritableScope memberScope;

    public NamespaceDescriptorImpl(@NotNull NamespaceDescriptorParent containingDeclaration,
                                   @NotNull List<AnnotationDescriptor> annotations,
                                   @NotNull Name name) {
        super(containingDeclaration, annotations, name);
    }

    public void initialize(@NotNull WritableScope memberScope) {
        if (this.memberScope != null) {
            throw new IllegalStateException("Namespace member scope reinitialize");
        }

        this.memberScope = memberScope;
    }

    @Override
    @NotNull
    public WritableScope getMemberScope() {
        return memberScope;
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        getMemberScope().addNamespace(namespaceDescriptor);
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return DescriptorUtils.getFQName(this).toSafe();
    }

    private NamespaceLikeBuilder builder = null;
    public NamespaceLikeBuilder getBuilder() {
        if (builder == null) {
            builder = new NamespaceLikeBuilder() {
                @Override
                public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
                    getMemberScope().addClassifierDescriptor(classDescriptor);
                }

                @Override
                public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {
                    getMemberScope().addObjectDescriptor(objectDescriptor);
                }

                @Override
                public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
                    getMemberScope().addFunctionDescriptor(functionDescriptor);
                }

                @Override
                public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
                    getMemberScope().addPropertyDescriptor(propertyDescriptor);
                }

                @NotNull
                @Override
                public DeclarationDescriptor getOwnerForChildren() {
                    return NamespaceDescriptorImpl.this;
                }

                @Override
                public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
                    throw new IllegalStateException("Must be guaranteed not to happen by the parser");
                }
            };
        }

        return builder;
    }
}
