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
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author abreslav
 */
public interface NamespaceLike extends DeclarationDescriptor {

    abstract class Adapter implements NamespaceLike {
        private final DeclarationDescriptor descriptor;

        public Adapter(DeclarationDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        @NotNull
        public DeclarationDescriptor getOriginal() {
            return descriptor.getOriginal();
        }

        @Override
        @Nullable
        public DeclarationDescriptor getContainingDeclaration() {
            return descriptor.getContainingDeclaration();
        }

        @Override
        public DeclarationDescriptor substitute(TypeSubstitutor substitutor) {
            return descriptor.substitute(substitutor);
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            return descriptor.accept(visitor, data);
        }

        @Override
        public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
            descriptor.acceptVoid(visitor);
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            return descriptor.getAnnotations();
        }

        @Override
        @NotNull
        public String getName() {
            return descriptor.getName();
        }
    }

    @Nullable
    NamespaceDescriptorImpl getNamespace(String name);

    void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor);

    void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor);

    void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor);

    void addFunctionDescriptor(@NotNull NamedFunctionDescriptor functionDescriptor);

    void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor);

    enum ClassObjectStatus {
        OK,
        DUPLICATE,
        NOT_ALLOWED
    }

    ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor);
}
