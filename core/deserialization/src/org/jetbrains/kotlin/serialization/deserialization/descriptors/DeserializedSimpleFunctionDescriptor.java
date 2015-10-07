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

package org.jetbrains.kotlin.serialization.deserialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.serialization.Flags;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.serialization.deserialization.Deserialization;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.serialization.deserialization.TypeTable;

public class DeserializedSimpleFunctionDescriptor extends SimpleFunctionDescriptorImpl implements DeserializedCallableMemberDescriptor {

    private final ProtoBuf.Function proto;
    private final NameResolver nameResolver;
    private final TypeTable typeTable;

    private DeserializedSimpleFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull ProtoBuf.Function proto,
            @NotNull NameResolver nameResolver,
            @NotNull TypeTable typeTable
    ) {
        super(containingDeclaration, original, annotations, name, kind, SourceElement.NO_SOURCE);
        this.proto = proto;
        this.nameResolver = nameResolver;
        this.typeTable = typeTable;
    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind
    ) {
        return new DeserializedSimpleFunctionDescriptor(
                newOwner,
                (DeserializedSimpleFunctionDescriptor) original,
                getAnnotations(),
                getName(),
                kind,
                proto,
                nameResolver,
                typeTable
        );
    }

    @NotNull
    @Override
    public DeserializedSimpleFunctionDescriptor getOriginal() {
        return (DeserializedSimpleFunctionDescriptor) super.getOriginal();
    }

    @NotNull
    @Override
    public ProtoBuf.Function getProto() {
        return proto;
    }

    @NotNull
    @Override
    public NameResolver getNameResolver() {
        return nameResolver;
    }

    @NotNull
    @Override
    public TypeTable getTypeTable() {
        return typeTable;
    }

    @NotNull
    public static DeserializedSimpleFunctionDescriptor create(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull ProtoBuf.Function proto,
            @NotNull NameResolver nameResolver,
            @NotNull TypeTable typeTable
    ) {
        return new DeserializedSimpleFunctionDescriptor(
                containingDeclaration,
                null,
                annotations,
                nameResolver.getName(proto.getName()),
                Deserialization.memberKind(Flags.MEMBER_KIND.get(proto.getFlags())),
                proto,
                nameResolver,
                typeTable
        );
    }
}
