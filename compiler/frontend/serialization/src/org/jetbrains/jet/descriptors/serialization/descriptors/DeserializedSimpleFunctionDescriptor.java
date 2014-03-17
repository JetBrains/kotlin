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
import org.jetbrains.jet.descriptors.serialization.DescriptorDeserializer;
import org.jetbrains.jet.descriptors.serialization.Flags;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;

public class DeserializedSimpleFunctionDescriptor extends SimpleFunctionDescriptorImpl {

    private final ProtoBuf.Callable functionProto;
    private final NameResolver nameResolver;

    private DeserializedSimpleFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull ProtoBuf.Callable functionProto,
            @NotNull NameResolver nameResolver
    ) {
        super(containingDeclaration, annotations, name, kind);
        this.functionProto = functionProto;
        this.nameResolver = nameResolver;
    }

    private DeserializedSimpleFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull ProtoBuf.Callable functionProto,
            @NotNull NameResolver nameResolver) {
        super(containingDeclaration, original, annotations, name, kind);
        this.functionProto = functionProto;
        this.nameResolver = nameResolver;
    }

    public DeserializedSimpleFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ProtoBuf.Callable functionProto,
            @NotNull Deserializers deserializers,
            @NotNull NameResolver nameResolver
    ) {
        this(containingDeclaration,
             DescriptorDeserializer.getAnnotations(containingDeclaration, functionProto, functionProto.getFlags(),
                                                   Deserializers.AnnotatedCallableKind.FUNCTION, deserializers.getAnnotationDeserializer(),
                                                   nameResolver),
             nameResolver.getName(functionProto.getName()),
             DescriptorDeserializer.memberKind(Flags.MEMBER_KIND.get(functionProto.getFlags())),
             functionProto,
             nameResolver);
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind) {
        if (preserveOriginal) {
            return new DeserializedSimpleFunctionDescriptor(
                    newOwner,
                    getOriginal(),
                    getAnnotations(),
                    getName(),
                    kind,
                    functionProto,
                    nameResolver
            );
        }
        else {
            return new DeserializedSimpleFunctionDescriptor(
                    newOwner,
                    getAnnotations(),
                    getName(),
                    kind,
                    functionProto,
                    nameResolver
            );
        }
    }

    @NotNull
    @Override
    public DeserializedSimpleFunctionDescriptor getOriginal() {
        return (DeserializedSimpleFunctionDescriptor) super.getOriginal();
    }

    public ProtoBuf.Callable getFunctionProto() {
        return functionProto;
    }

    public NameResolver getNameResolver() {
        return nameResolver;
    }
}
