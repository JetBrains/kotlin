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

import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;

import static org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager.ReferenceKind.STRONG;

public abstract class AbstractDescriptorFinder implements DescriptorFinder {

    private final MemoizedFunctionToNullable<ClassId, ClassDescriptor> findClass;
    private final AnnotationDeserializer annotationDeserializer;

    public AbstractDescriptorFinder(@NotNull final StorageManager storageManager, @NotNull AnnotationDeserializer annotationDeserializer) {
        this.annotationDeserializer = annotationDeserializer;

        this.findClass = storageManager.createMemoizedFunctionWithNullableValues(new Function<ClassId, ClassDescriptor>() {
            @Override
            public ClassDescriptor fun(ClassId classId) {
                ClassData classData = getClassData(classId);
                if (classData == null) {
                    return null;
                }

                ProtoBuf.Class classProto = classData.getClassProto();

                DeclarationDescriptor owner =
                        classId.isTopLevelClass() ? getPackage(classId.getPackageFqName()) : findClass(classId.getOuterClassId());
                assert owner != null : "No owner found for " + classId;

                AbstractDescriptorFinder _this = AbstractDescriptorFinder.this;
                ClassDescriptor classDescriptor = new DeserializedClassDescriptor(
                        classId, storageManager, owner, classData.getNameResolver(),
                        _this.annotationDeserializer, _this, classProto, null);
                classDescriptorCreated(classDescriptor);
                return classDescriptor;
            }
        }, STRONG);
    }

    @Nullable
    @Override
    public ClassDescriptor findClass(@NotNull ClassId classId) {
        ClassDescriptor externalClassDescriptor = resolveClassExternally(classId);
        if (externalClassDescriptor != null) {
            return externalClassDescriptor;
        }
        return findClassInternally(classId);
    }

    //do not call resolveClassExternally
    @Nullable
    public ClassDescriptor findClassInternally(@NotNull ClassId classId) {
        return findClass.fun(classId);
    }

    @Nullable
    protected abstract ClassData getClassData(@NotNull ClassId classId);

    @NotNull
    protected abstract DeclarationDescriptor getPackage(@NotNull FqName fqName);

    @Nullable
    protected ClassDescriptor resolveClassExternally(@NotNull ClassId classId) {
        //TODO: decide whether it is ok to provide default implementation
        return null;
    }

    protected abstract void classDescriptorCreated(@NotNull ClassDescriptor classDescriptor);
}
