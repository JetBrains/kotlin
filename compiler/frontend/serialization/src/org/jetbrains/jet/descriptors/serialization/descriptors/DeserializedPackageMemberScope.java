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

package org.jetbrains.jet.descriptors.serialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.storage.StorageManager;

import java.util.Collection;
import java.util.Collections;

public class DeserializedPackageMemberScope extends DeserializedMemberScope {
    private final DescriptorFinder descriptorFinder;

    private final FqName packageFqName;

    public DeserializedPackageMemberScope(
            @NotNull StorageManager storageManager,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull AnnotationDeserializer annotationDeserializer,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull ProtoBuf.Package proto,
            @NotNull NameResolver nameResolver
    ) {
        super(storageManager, packageDescriptor,
              DescriptorDeserializer.create(storageManager, packageDescriptor, nameResolver, descriptorFinder, annotationDeserializer),
              proto.getMemberList());
        this.descriptorFinder = descriptorFinder;
        this.packageFqName = DescriptorUtils.getFQName(packageDescriptor).toSafe();
    }

    public DeserializedPackageMemberScope(
            @NotNull StorageManager storageManager,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull AnnotationDeserializer annotationDeserializer,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull PackageData packageData
    ) {
        this(storageManager, packageDescriptor, annotationDeserializer, descriptorFinder, packageData.getPackageProto(),
             packageData.getNameResolver());
    }

    @Nullable
    @Override
    protected ClassifierDescriptor getClassDescriptor(@NotNull Name name) {
        return findClassDescriptor(name);
    }

    @Nullable
    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        return null;
    }

    @Nullable
    private ClassDescriptor findClassDescriptor(Name name) {
        return descriptorFinder.findClass(new ClassId(packageFqName, FqNameUnsafe.topLevel(name)));
    }

    @Override
    protected void addAllClassDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        for (Name className : descriptorFinder.getClassNames(packageFqName)) {
            ClassDescriptor classDescriptor = findClassDescriptor(className);

            if (classDescriptor != null) {
                result.add(classDescriptor);
            }
        }
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeAllObjectDescriptors() {
        return Collections.emptySet();
    }

    @Override
    protected void addNonDeclaredDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        // Do nothing
    }

    @Nullable
    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return descriptorFinder.findPackage(packageFqName.child(name));
    }

    @Nullable
    @Override
    protected ReceiverParameterDescriptor getImplicitReceiver() {
        return null;
    }
}
