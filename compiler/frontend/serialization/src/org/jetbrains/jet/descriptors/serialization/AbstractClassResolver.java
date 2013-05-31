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
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNotNullImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public abstract class AbstractClassResolver implements ClassResolver {

    private final NameResolver nameResolver;
    private final NestedClassResolver nestedClassResolver;
    private final MemoizedFunctionToNotNull<ClassId, ClassDescriptor> findClass;

    public AbstractClassResolver(@NotNull NameResolver nameResolver) {
        this.nameResolver = nameResolver;

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

        this.findClass = new MemoizedFunctionToNotNullImpl<ClassId, ClassDescriptor>() {
            @NotNull
            @Override
            protected ClassDescriptor doCompute(ClassId classId) {
                ProtoBuf.Class classProto = getClassProto(classId);
                assert classProto != null : "No class found: " + classId;
                DeclarationDescriptor owner =
                        classId.isTopLevelClass() ? getPackage(classId.getPackageFqName()) : findClass(classId.getOuterClassId());
                assert owner != null : "No owner found for " + classId;
                ClassDescriptor classDescriptor = new DeserializedClassDescriptor(
                        owner, AbstractClassResolver.this.nameResolver, AbstractClassResolver.this, nestedClassResolver, classProto, null
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
    protected abstract ProtoBuf.Class getClassProto(@NotNull ClassId classId);

    @NotNull
    protected abstract DeclarationDescriptor getPackage(@NotNull FqName fqName);

    @NotNull
    protected abstract ClassId getClassId(@NotNull ClassDescriptor classDescriptor);

    @NotNull
    protected abstract Name getClassObjectName(@NotNull ClassDescriptor outerClass);

    protected abstract void classDescriptorCreated(@NotNull ClassDescriptor classDescriptor);
}
