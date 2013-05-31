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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullableImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public abstract class AbstractClassResolver implements ClassResolver {

    private final NestedClassResolver nestedClassResolver;
    private final MemoizedFunctionToNullable<ClassId, ClassDescriptor> findClass;
    private final AnnotationDeserializer annotationDeserializer;

    public AbstractClassResolver(@NotNull AnnotationDeserializer annotationDeserializer) {
        this.annotationDeserializer = annotationDeserializer;

        this.nestedClassResolver = new NestedClassResolver() {
            @Nullable
            @Override
            public ClassDescriptor resolveNestedClass(@NotNull ClassDescriptor outerClass, @NotNull Name name) {
                return findClass(getClassId(outerClass).createNestedClassId(name));
            }

            @Nullable
            @Override
            public ClassDescriptor resolveClassObject(@NotNull ClassDescriptor outerClass) {
                return findClass(getClassId(outerClass).createNestedClassId(getClassObjectName(outerClass)));
            }
        };

        this.findClass = new MemoizedFunctionToNullableImpl<ClassId, ClassDescriptor>() {
            @Override
            protected ClassDescriptor doCompute(ClassId classId) {
                ClassData classData = getClassData(classId);
                if (classData == null) {
                    return null;
                }

                ProtoBuf.Class classProto = classData.getClassProto();

                DeclarationDescriptor owner =
                        classId.isTopLevelClass() ? getPackage(classId.getPackageFqName()) : findClass(classId.getOuterClassId());
                assert owner != null : "No owner found for " + classId;

                AbstractClassResolver outer = AbstractClassResolver.this;
                ClassDescriptor classDescriptor = new DeserializedClassDescriptor(
                        owner, classData.getNameResolver(), outer.annotationDeserializer, outer, nestedClassResolver, classProto, null
                );
                classDescriptorCreated(classDescriptor);
                return classDescriptor;
            }
        };
    }

    @Nullable
    @Override
    public ClassDescriptor findClass(@NotNull ClassId classId) {
        return findClass.fun(classId);
    }

    @Nullable
    protected abstract ClassData getClassData(@NotNull ClassId classId);

    @NotNull
    protected abstract DeclarationDescriptor getPackage(@NotNull FqName fqName);

    @NotNull
    protected abstract ClassId getClassId(@NotNull ClassDescriptor classDescriptor);

    @NotNull
    protected abstract Name getClassObjectName(@NotNull ClassDescriptor outerClass);

    protected abstract void classDescriptorCreated(@NotNull ClassDescriptor classDescriptor);

    protected static class ClassData {
        private final NameResolver nameResolver;
        private final ProtoBuf.Class classProto;

        public ClassData(@NotNull NameResolver nameResolver, @NotNull ProtoBuf.Class classProto) {
            this.nameResolver = nameResolver;
            this.classProto = classProto;
        }

        @NotNull
        public NameResolver getNameResolver() {
            return nameResolver;
        }

        @NotNull
        public ProtoBuf.Class getClassProto() {
            return classProto;
        }
    }
}
