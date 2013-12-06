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

import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.storage.StorageManager;

public abstract class AbstractDescriptorFinder implements DescriptorFinder {
    private final MemoizedFunctionToNullable<ClassId, ClassDescriptor> findClass;
    private final AnnotationDeserializer annotationDeserializer;

    public AbstractDescriptorFinder(
            @NotNull final StorageManager storageManager,
            @NotNull AnnotationDeserializer annotationDeserializer,
            @NotNull final PackageFragmentProvider packageFragmentProvider
    ) {
        this.annotationDeserializer = annotationDeserializer;

        this.findClass = storageManager.createMemoizedFunctionWithNullableValues(new Function1<ClassId, ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke(ClassId classId) {
                ClassData classData = getClassData(classId);
                if (classData == null) {
                    return null;
                }

                AbstractDescriptorFinder _this = AbstractDescriptorFinder.this;
                return new DeserializedClassDescriptor(storageManager, _this.annotationDeserializer, _this, packageFragmentProvider,
                                                classData.getNameResolver(), classData.getClassProto());
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
