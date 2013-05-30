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

import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TypeDeserializer {
    private final NameResolver nameResolver;
    private final ClassResolver classResolver;
    private final IndexedSymbolTable<TypeParameterDescriptor> typeParameterDescriptors;
    private final TIntObjectHashMap<ClassDescriptor> classDescriptors = new TIntObjectHashMap<ClassDescriptor>();

    public TypeDeserializer(@NotNull TypeDeserializer parent) {
        this(parent, parent.nameResolver, parent.classResolver);
    }

    public TypeDeserializer(
            @Nullable TypeDeserializer parent,
            @NotNull NameResolver nameResolver,
            @NotNull ClassResolver classResolver
    ) {
        IndexedSymbolTable<TypeParameterDescriptor> parentTypeParameters = parent == null ? null : parent.typeParameterDescriptors;
        this.typeParameterDescriptors = new IndexedSymbolTable<TypeParameterDescriptor>(parentTypeParameters);
        this.nameResolver = nameResolver;
        this.classResolver = classResolver;
    }

    @NotNull
    public ClassResolver getClassResolver() {
        return classResolver;
    }

    public void registerTypeParameter(int id, TypeParameterDescriptor typeParameter) {
        typeParameterDescriptors.registerSymbol(id, typeParameter);
    }

    @Nullable
    public JetType typeOrNull(@Nullable ProtoBuf.Type proto) {
        if (proto == null) {
            return null;
        }
        return type(proto);
    }

    @NotNull
    public JetType type(@NotNull final ProtoBuf.Type proto) {
        // Types are lazy
        return new JetType() {
            private TypeConstructor constructor;
            private final List<TypeProjection> arguments = typeArguments(proto.getArgumentsList());
            private JetScope memberScope;

            @NotNull
            @Override
            public TypeConstructor getConstructor() {
                if (constructor == null) {
                    constructor = typeConstructor(proto);
                }
                return constructor;
            }

            @NotNull
            @Override
            public List<TypeProjection> getArguments() {
                return arguments;
            }

            @Override
            public boolean isNullable() {
                return proto.getNullable();
            }

            @NotNull
            @Override
            public JetScope getMemberScope() {
                if (memberScope == null) {
                    TypeConstructor typeConstructor = getConstructor();
                    if (ErrorUtils.isError(typeConstructor)) {
                        memberScope = ErrorUtils.createErrorScope(typeConstructor.toString());
                    }
                    else {
                        memberScope = getTypeMemberScope(typeConstructor, getArguments());
                    }
                }
                return memberScope;
            }

            @Override
            public List<AnnotationDescriptor> getAnnotations() {
                return Collections.emptyList();
            }

            @Override
            public String toString() {
                return TypeUtils.toString(this);
            }
        };
    }

    private TypeConstructor typeConstructor(ProtoBuf.Type proto) {
        ProtoBuf.Type.Constructor constructorProto = proto.getConstructor();
        int id = constructorProto.getId();
        TypeConstructor typeConstructor = typeConstructor(constructorProto);
        if (typeConstructor == null) {
            String message = constructorProto.getKind() == ProtoBuf.Type.Constructor.Kind.CLASS
                             ? nameResolver.getClassId(id).asSingleFqName().asString()
                             : "Unknown type parameter " + id;
            typeConstructor = ErrorUtils.createErrorType(message).getConstructor();
        }
        return typeConstructor;
    }

    @Nullable
    private TypeConstructor typeConstructor(@NotNull ProtoBuf.Type.Constructor proto) {
        switch (proto.getKind()) {
            case CLASS:
                ClassDescriptor classDescriptor = getClassDescriptor(proto.getId());
                if (classDescriptor == null) return null;

                return classDescriptor.getTypeConstructor();
            case TYPE_PARAMETER:
                TypeParameterDescriptor descriptor = typeParameterDescriptors.getSymbol(proto.getId());
                if (descriptor == null) return null;

                return descriptor.getTypeConstructor();
        }
        throw new IllegalStateException("Unknown kind " + proto.getKind());
    }

    @Nullable
    private ClassDescriptor getClassDescriptor(int fqNameIndex) {
        ClassDescriptor classDescriptor = classDescriptors.get(fqNameIndex);
        if (classDescriptor != null) {
            return classDescriptor;
        }

        ClassId classId = nameResolver.getClassId(fqNameIndex);
        ClassDescriptor descriptor = classResolver.findClass(classId);
        classDescriptors.put(fqNameIndex, descriptor);
        return descriptor;
    }

    private List<TypeProjection> typeArguments(List<ProtoBuf.Type.Argument> protos) {
        List<TypeProjection> result = new ArrayList<TypeProjection>(protos.size());
        for (ProtoBuf.Type.Argument proto : protos) {
            result.add(typeProjection(proto));
        }
        return result;
    }

    private TypeProjection typeProjection(ProtoBuf.Type.Argument proto) {
        return new TypeProjection(
                variance(proto.getProjection()),
                type(proto.getType())
        );
    }

    private static Variance variance(ProtoBuf.Type.Argument.Projection proto) {
        switch (proto) {
            case IN:
                return Variance.IN_VARIANCE;
            case OUT:
                return Variance.OUT_VARIANCE;
            case INV:
                return Variance.INVARIANT;
        }
        throw new IllegalStateException("Unknown projection: " + proto);
    }

    @NotNull
    private static JetScope getTypeMemberScope(@NotNull TypeConstructor constructor, @NotNull List<TypeProjection> typeArguments) {
        ClassifierDescriptor descriptor = constructor.getDeclarationDescriptor();
        if (descriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) descriptor;
            return typeParameterDescriptor.getDefaultType().getMemberScope();
        }
        return ((ClassDescriptor) descriptor).getMemberScope(typeArguments);
    }
}
