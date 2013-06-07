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
import java.util.Collections;

public class RecursiveDescriptorProcessor {
    public static <D> boolean process(
            @NotNull Collection<? extends DeclarationDescriptor> descriptors,
            D data,
            @NotNull DeclarationDescriptorVisitor<Boolean, D> visitor
    ) {
        RecursiveVisitor<D> recursive = new RecursiveVisitor<D>(visitor);
        for (DeclarationDescriptor descriptor : descriptors) {
            if (!descriptor.accept(visitor, data)) {
                return false;
            }
            descriptor.accept(recursive, data);
        }
        return true;
    }

    public static <D> boolean process(
            @NotNull DeclarationDescriptor descriptor,
            D data,
            @NotNull DeclarationDescriptorVisitor<Boolean, D> visitor
    ) {
        return process(Collections.singletonList(descriptor), data, visitor);
    }

    private static class RecursiveVisitor<D> implements DeclarationDescriptorVisitor<Boolean, D> {

        private final DeclarationDescriptorVisitor<Boolean, D> worker;

        private RecursiveVisitor(@NotNull DeclarationDescriptorVisitor<Boolean, D> worker) {
            this.worker = worker;
        }

        private boolean doProcess(Collection<? extends  DeclarationDescriptor> descriptors, D data) {
            return process(descriptors, data, worker);
        }

        private boolean doProcess(@Nullable DeclarationDescriptor receiverParameter, D data) {
            if (receiverParameter == null) {
                return true;
            }
            return receiverParameter.accept(worker, data);
        }

        private boolean processCallable(CallableDescriptor descriptor, D data) {
            return doProcess(descriptor.getTypeParameters(), data)
                    && doProcess(descriptor.getReceiverParameter(), data)
                    && doProcess(descriptor.getValueParameters(), data);
        }

        @Override
        public Boolean visitNamespaceDescriptor(NamespaceDescriptor descriptor, D data) {
            return doProcess(descriptor.getMemberScope().getAllDescriptors(), data);
        }

        @Override
        public Boolean visitVariableDescriptor(VariableDescriptor descriptor, D data) {
            return processCallable(descriptor, data);
        }

        @Override
        public Boolean visitPropertyDescriptor(PropertyDescriptor descriptor, D data) {
            return processCallable(descriptor, data)
                    && doProcess(descriptor.getGetter(), data)
                    && doProcess(descriptor.getSetter(), data);
        }

        @Override
        public Boolean visitFunctionDescriptor(FunctionDescriptor descriptor, D data) {
            return processCallable(descriptor, data);
        }

        @Override
        public Boolean visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, D data) {
            return true;
        }

        @Override
        public Boolean visitClassDescriptor(ClassDescriptor descriptor, D data) {
            return doProcess(descriptor.getThisAsReceiverParameter(), data)
                    && doProcess(descriptor.getConstructors(), data)
                    && doProcess(descriptor.getTypeConstructor().getParameters(), data)
                    && doProcess(descriptor.getClassObjectDescriptor(), data)
                    && doProcess(descriptor.getDefaultType().getMemberScope().getObjectDescriptors(), data)
                    && doProcess(descriptor.getDefaultType().getMemberScope().getAllDescriptors(), data);
        }

        @Override
        public Boolean visitModuleDeclaration(ModuleDescriptor descriptor, D data) {
            return doProcess(descriptor.getNamespace(FqName.ROOT), data);
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
            return true;
        }
    }
}
