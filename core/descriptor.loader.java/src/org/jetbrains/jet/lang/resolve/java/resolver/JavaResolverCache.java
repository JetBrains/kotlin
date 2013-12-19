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

package org.jetbrains.jet.lang.resolve.java.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.name.FqName;

public interface JavaResolverCache {
    @Nullable
    ClassDescriptor getClassResolvedFromSource(@NotNull FqName fqName);

    @Nullable
    SimpleFunctionDescriptor getMethod(@NotNull JavaMethod method);

    @Nullable
    ConstructorDescriptor getConstructor(@NotNull JavaElement constructor);

    @Nullable
    ClassDescriptor getClass(@NotNull JavaClass javaClass);

    void recordMethod(@NotNull JavaMethod method, @NotNull SimpleFunctionDescriptor descriptor);

    void recordConstructor(@NotNull JavaElement element, @NotNull ConstructorDescriptor descriptor);

    void recordField(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor);

    void recordClass(@NotNull JavaClass javaClass, @NotNull ClassDescriptor descriptor);

    void recordProperPackage(@NotNull PackageFragmentDescriptor descriptor);

    void recordClassStaticMembersNamespace(@NotNull PackageFragmentDescriptor descriptor);
}
