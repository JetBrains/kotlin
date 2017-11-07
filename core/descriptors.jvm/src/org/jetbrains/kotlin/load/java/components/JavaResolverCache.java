/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.components;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.java.structure.JavaElement;
import org.jetbrains.kotlin.load.java.structure.JavaField;
import org.jetbrains.kotlin.load.java.structure.JavaMethod;
import org.jetbrains.kotlin.name.FqName;

public interface JavaResolverCache {
    JavaResolverCache EMPTY = new JavaResolverCache() {
        @Nullable
        @Override
        public ClassDescriptor getClassResolvedFromSource(@NotNull FqName fqName) {
            return null;
        }

        @Override
        public void recordMethod(@NotNull JavaMethod method, @NotNull SimpleFunctionDescriptor descriptor) {
        }

        @Override
        public void recordConstructor(@NotNull JavaElement element, @NotNull ConstructorDescriptor descriptor) {
        }

        @Override
        public void recordField(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor) {
        }

        @Override
        public void recordClass(@NotNull JavaClass javaClass, @NotNull ClassDescriptor descriptor) {
        }
    };

    @Nullable
    ClassDescriptor getClassResolvedFromSource(@NotNull FqName fqName);

    void recordMethod(@NotNull JavaMethod method, @NotNull SimpleFunctionDescriptor descriptor);

    void recordConstructor(@NotNull JavaElement element, @NotNull ConstructorDescriptor descriptor);

    void recordField(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor);

    void recordClass(@NotNull JavaClass javaClass, @NotNull ClassDescriptor descriptor);
}
