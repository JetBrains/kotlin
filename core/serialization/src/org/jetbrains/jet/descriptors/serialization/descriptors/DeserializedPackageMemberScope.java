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
import org.jetbrains.jet.descriptors.serialization.ClassId;
import org.jetbrains.jet.descriptors.serialization.PackageData;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.context.ContextPackage;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContext;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationGlobalContext;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

public class DeserializedPackageMemberScope extends DeserializedMemberScope {

    private final FqName packageFqName;
    private final DeserializationContext context;

    public DeserializedPackageMemberScope(
            @NotNull PackageFragmentDescriptor packageDescriptor,
            @NotNull ProtoBuf.Package proto,
            @NotNull DeserializationContext context
    ) {
        super(context.withTypes(packageDescriptor), proto.getMemberList());
        this.context = context;
        this.packageFqName = packageDescriptor.getFqName();
    }

    public DeserializedPackageMemberScope(
            @NotNull PackageFragmentDescriptor packageDescriptor,
            @NotNull PackageData packageData,
            @NotNull DeserializationGlobalContext context
    ) {
        this(packageDescriptor, packageData.getPackageProto(), context.withNameResolver(packageData.getNameResolver()));
    }
    @Nullable
    @Override
    protected ClassDescriptor getClassDescriptor(@NotNull Name name) {
        return ContextPackage.deserializeClass(context, new ClassId(packageFqName, FqNameUnsafe.topLevel(name)));
    }

    @Override
    protected void addAllClassDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        for (Name className : context.getClassDataFinder().getClassNames(packageFqName)) {
            ClassDescriptor classDescriptor = getClassDescriptor(className);

            if (classDescriptor != null) {
                result.add(classDescriptor);
            }
        }
    }

    @Override
    protected void addNonDeclaredDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        // Do nothing
    }

    @Nullable
    @Override
    protected ReceiverParameterDescriptor getImplicitReceiver() {
        return null;
    }
}
