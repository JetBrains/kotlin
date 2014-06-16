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

package org.jetbrains.jet.descriptors.serialization;

import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationGlobalContext;
import org.jetbrains.jet.descriptors.serialization.descriptors.*;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.storage.StorageManager;

public abstract class AbstractDescriptorFinder implements DescriptorFinder {
    private final MemoizedFunctionToNullable<ClassId, ClassDescriptor> findClass;

    public AbstractDescriptorFinder(
            @NotNull StorageManager storageManager,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull AnnotationLoader annotationLoader,
            @NotNull ConstantLoader constantLoader,
            @NotNull PackageFragmentProvider packageFragmentProvider
    ) {
        final DeserializationGlobalContext deserializationGlobalContext =
                new DeserializationGlobalContext(storageManager, moduleDescriptor, this, annotationLoader, constantLoader,
                                                 packageFragmentProvider);
        this.findClass = storageManager.createMemoizedFunctionWithNullableValues(new Function1<ClassId, ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke(ClassId classId) {
                ClassData classData = getClassData(classId);
                return classData == null ? null : new DeserializedClassDescriptor(deserializationGlobalContext, classData);
            }
        });
    }

    @Nullable
    @Override
    public ClassDescriptor findClass(@NotNull ClassId classId) {
        return findClass.invoke(classId);
    }

    @Nullable
    protected abstract ClassData getClassData(@NotNull ClassId classId);
}
