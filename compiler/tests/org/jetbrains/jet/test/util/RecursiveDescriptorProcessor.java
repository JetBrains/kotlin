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

package org.jetbrains.jet.test.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public class RecursiveDescriptorProcessor {

    public static <D> boolean process(
            @NotNull DeclarationDescriptor descriptor,
            D data,
            @NotNull DeclarationDescriptorVisitor<Boolean, D> visitor
    ) {
        return descriptor.accept(new RecursiveVisitor<D>(visitor), data);
    }

    private static class RecursiveVisitor<D> implements DeclarationDescriptorVisitor<Boolean, D> {

        private final DeclarationDescriptorVisitor<Boolean, D> worker;

        private RecursiveVisitor(@NotNull DeclarationDescriptorVisitor<Boolean, D> worker) {
            this.worker = worker;
        }

        private boolean visitChildren(Collection<? extends DeclarationDescriptor> descriptors, D data) {
            for (DeclarationDescriptor descriptor : descriptors) {
                if (!descriptor.accept(this, data)) return false;
            }
            return true;
        }

        private boolean visitChildren(@Nullable DeclarationDescriptor descriptor, D data) {
            if (descriptor == null) return true;

            return descriptor.accept(this, data);
        }

        private boolean applyWorker(@NotNull DeclarationDescriptor descriptor, D data) {
            return descriptor.accept(worker, data);
        }

        private boolean processCallable(CallableDescriptor descriptor, D data) {
            return applyWorker(descriptor, data)
                   && visitChildren(descriptor.getTypeParameters(), data)
                   && visitChildren(descriptor.getReceiverParameter(), data)
                   && visitChildren(descriptor.getValueParameters(), data);
        }

        @Override
        public Boolean visitPackageFragmentDescriptor(PackageFragmentDescriptor descriptor, D data) {
            return applyWorker(descriptor, data)
                   && visitChildren(descriptor.getMemberScope().getAllDescriptors(), data);
        }

        @Override
        public Boolean visitPackageViewDescriptor(PackageViewDescriptor descriptor, D data) {
            return applyWorker(descriptor, data)
                   && visitChildren(descriptor.getMemberScope().getAllDescriptors(), data);
        }

        @Override
        public Boolean visitVariableDescriptor(VariableDescriptor descriptor, D data) {
            return processCallable(descriptor, data);
        }

        @Override
        public Boolean visitPropertyDescriptor(PropertyDescriptor descriptor, D data) {
            return processCallable(descriptor, data)
                   && visitChildren(descriptor.getGetter(), data)
                   && visitChildren(descriptor.getSetter(), data);
        }

        @Override
        public Boolean visitFunctionDescriptor(FunctionDescriptor descriptor, D data) {
            return processCallable(descriptor, data);
        }

        @Override
        public Boolean visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, D data) {
            return applyWorker(descriptor, data);
        }

        @Override
        public Boolean visitClassDescriptor(ClassDescriptor descriptor, D data) {
            return applyWorker(descriptor, data)
                   && visitChildren(descriptor.getThisAsReceiverParameter(), data)
                   && visitChildren(descriptor.getConstructors(), data)
                   && visitChildren(descriptor.getTypeConstructor().getParameters(), data)
                   && visitChildren(descriptor.getClassObjectDescriptor(), data)
                   && visitChildren(descriptor.getDefaultType().getMemberScope().getAllDescriptors(), data);
        }

        @Override
        public Boolean visitModuleDeclaration(ModuleDescriptor descriptor, D data) {
            return applyWorker(descriptor, data)
                   && visitChildren(descriptor.getPackage(FqName.ROOT), data);
        }

        @Override
        public Boolean visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, D data) {
            return visitFunctionDescriptor(constructorDescriptor, data);
        }

        @Override
        public Boolean visitScriptDescriptor(ScriptDescriptor scriptDescriptor, D data) {
            return visitClassDescriptor(scriptDescriptor.getClassDescriptor(), data);
        }

        @Override
        public Boolean visitValueParameterDescriptor(ValueParameterDescriptor descriptor, D data) {
            return visitVariableDescriptor(descriptor, data);
        }

        @Override
        public Boolean visitPropertyGetterDescriptor(PropertyGetterDescriptor descriptor, D data) {
            return visitFunctionDescriptor(descriptor, data);
        }

        @Override
        public Boolean visitPropertySetterDescriptor(PropertySetterDescriptor descriptor, D data) {
            return visitFunctionDescriptor(descriptor, data);
        }

        @Override
        public Boolean visitReceiverParameterDescriptor(ReceiverParameterDescriptor descriptor, D data) {
            return applyWorker(descriptor, data);
        }
    }
}
