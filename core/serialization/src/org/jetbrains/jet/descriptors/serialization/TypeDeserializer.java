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

import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationComponents;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContext;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedTypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.utils.UtilsPackage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.descriptors.serialization.SerializationPackage.variance;

public class TypeDeserializer {
    private final DeserializationContext context;
    private final TypeDeserializer parent;
    private final Map<Integer, TypeParameterDescriptor> typeParameterDescriptors = new LinkedHashMap<Integer, TypeParameterDescriptor>();
    private final MemoizedFunctionToNullable<Integer, ClassDescriptor> classDescriptors;

    private final String debugName;

    public TypeDeserializer(@NotNull DeserializationContext context, @Nullable TypeDeserializer parent, @NotNull String debugName) {
        this.parent = parent;
        this.debugName = debugName + (parent == null ? "" : ". Child of " + parent.debugName);
        this.context = context;

        this.classDescriptors = getComponents().getStorageManager().createMemoizedFunctionWithNullableValues(
                new Function1<Integer, ClassDescriptor>() {
                    @Override
                    public ClassDescriptor invoke(Integer fqNameIndex) {
                        return computeClassDescriptor(fqNameIndex);
                    }
                });
    }

    @NotNull
    private DeserializationComponents getComponents() {
        return context.getComponents();
    }

    public void addTypeParameter(@NotNull DeserializedTypeParameterDescriptor typeParameterDescriptor) {
        typeParameterDescriptors.put(typeParameterDescriptor.getProtoId(), typeParameterDescriptor);
    }

    @NotNull
    public List<TypeParameterDescriptor> getOwnTypeParameters() {
        return UtilsPackage.toReadOnlyList(typeParameterDescriptors.values());
    }

    @NotNull
    public JetType type(@NotNull ProtoBuf.Type proto) {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            String id = context.getNameResolver().getString(proto.getFlexibleTypeCapabilitiesId());
            FlexibleTypeCapabilities capabilities = getComponents().getFlexibleTypeCapabilitiesDeserializer().capabilitiesById(id);

            if (capabilities == null) {
                return ErrorUtils.createErrorType(new DeserializedType(context, proto) + ": Capabilities not found for id " + id);
            }

            return DelegatingFlexibleType.create(
                    new DeserializedType(context, proto),
                    new DeserializedType(context, proto.getFlexibleUpperBound()),
                    capabilities
            );
        }

        return new DeserializedType(context, proto);
    }

    @NotNull
    public TypeConstructor typeConstructor(@NotNull ProtoBuf.Type proto) {
        ProtoBuf.Type.Constructor constructorProto = proto.getConstructor();
        int id = constructorProto.getId();
        TypeConstructor typeConstructor = typeConstructor(constructorProto);
        if (typeConstructor == null) {
            String message = constructorProto.getKind() == ProtoBuf.Type.Constructor.Kind.CLASS
                             ? context.getNameResolver().getClassId(id).asSingleFqName().asString()
                             : "Unknown type parameter " + id;
            return ErrorUtils.createErrorType(message).getConstructor();
        }
        return typeConstructor;
    }

    @Nullable
    private TypeConstructor typeConstructor(@NotNull ProtoBuf.Type.Constructor proto) {
        switch (proto.getKind()) {
            case CLASS:
                ClassDescriptor classDescriptor = classDescriptors.invoke(proto.getId());
                if (classDescriptor == null) return null;

                return classDescriptor.getTypeConstructor();
            case TYPE_PARAMETER:
                return typeParameterTypeConstructor(proto);
        }
        throw new IllegalStateException("Unknown kind " + proto.getKind());
    }

    @Nullable
    private TypeConstructor typeParameterTypeConstructor(@NotNull ProtoBuf.Type.Constructor proto) {
        TypeParameterDescriptor descriptor = typeParameterDescriptors.get(proto.getId());
        if (descriptor != null) {
            return descriptor.getTypeConstructor();
        }

        if (parent != null) {
            return parent.typeParameterTypeConstructor(proto);
        }

        return null;
    }

    @Nullable
    private ClassDescriptor computeClassDescriptor(int fqNameIndex) {
        return SerializationPackage.findClassAcrossModuleDependencies(
                getComponents().getModuleDescriptor(),
                context.getNameResolver().getClassId(fqNameIndex)
        );
    }

    @NotNull
    public List<TypeProjection> typeArguments(@NotNull List<ProtoBuf.Type.Argument> protos) {
        List<TypeProjection> result = new ArrayList<TypeProjection>(protos.size());
        for (ProtoBuf.Type.Argument proto : protos) {
            result.add(typeProjection(proto));
        }
        return result;
    }

    private TypeProjection typeProjection(ProtoBuf.Type.Argument proto) {
        return new TypeProjectionImpl(variance(proto.getProjection()), type(proto.getType()));
    }

    @Override
    public String toString() {
        return debugName;
    }
}
