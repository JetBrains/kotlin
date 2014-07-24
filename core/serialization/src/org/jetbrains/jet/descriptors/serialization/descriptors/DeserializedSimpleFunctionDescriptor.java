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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;

public class DeserializedSimpleFunctionDescriptor extends SimpleFunctionDescriptorImpl implements DeserializedCallableMemberDescriptor {

    private final ProtoBuf.Callable proto;
    private final NameResolver nameResolver;

    private DeserializedSimpleFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver) {
        super(containingDeclaration, original, annotations, name, kind, SourceElement.NO_SOURCE);
        this.proto = proto;
        this.nameResolver = nameResolver;
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
                nameResolver
        );
    }

    @NotNull
    @Override
    public DeserializedSimpleFunctionDescriptor getOriginal() {
        return (DeserializedSimpleFunctionDescriptor) super.getOriginal();
    }

    @NotNull
    @Override
    public ProtoBuf.Callable getProto() {
        return proto;
    }

    @NotNull
    @Override
    public NameResolver getNameResolver() {
        return nameResolver;
    }

    public static DeserializedSimpleFunctionDescriptor create(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ProtoBuf.Callable proto,
            @NotNull AnnotationLoader annotationLoader,
            @NotNull NameResolver nameResolver
    ) {
        Annotations annotations = MemberDeserializer.getAnnotations(containingDeclaration, proto, proto.getFlags(),
                                                                    AnnotatedCallableKind.FUNCTION,
                                                                    annotationLoader,
                                                                    nameResolver);
        return new DeserializedSimpleFunctionDescriptor(
                containingDeclaration,
                null,
                annotations,
                nameResolver.getName(proto.getName()),
                SerializationPackage.memberKind(Flags.MEMBER_KIND.get(proto.getFlags())),
                proto,
                nameResolver);
    }
}
