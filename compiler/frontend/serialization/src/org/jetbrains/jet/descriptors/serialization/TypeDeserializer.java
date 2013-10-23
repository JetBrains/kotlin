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
import jet.Function0;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedTypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TypeDeserializer {

    public interface TypeParameterResolver {
        TypeParameterResolver NONE = new TypeParameterResolver() {
            @NotNull
            @Override
            public List<DeserializedTypeParameterDescriptor> getTypeParameters(@NotNull TypeDeserializer typeDeserializer) {
                return Collections.emptyList();
            }
        };

        @NotNull
        List<DeserializedTypeParameterDescriptor> getTypeParameters(@NotNull TypeDeserializer typeDeserializer);
    }

    private final NameResolver nameResolver;
    private final DescriptorFinder descriptorFinder;
    private final TypeDeserializer parent;

    // never written to after constructor returns
    private final TIntObjectHashMap<TypeParameterDescriptor> typeParameterDescriptors = new TIntObjectHashMap<TypeParameterDescriptor>();

    private final MemoizedFunctionToNullable<Integer, ClassDescriptor> classDescriptors;

    private final String debugName;

    private final StorageManager storageManager;

    public TypeDeserializer(
            @NotNull StorageManager storageManager,
            @NotNull TypeDeserializer parent,
            @NotNull String debugName,
            @NotNull TypeParameterResolver typeParameterResolver
    ) {
        this(storageManager, parent, parent.nameResolver, parent.descriptorFinder, debugName, typeParameterResolver);
    }

    public TypeDeserializer(
            @NotNull StorageManager storageManager,
            @Nullable TypeDeserializer parent,
            @NotNull NameResolver nameResolver,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull String debugName,
            @NotNull TypeParameterResolver typeParameterResolver
    ) {
        this.storageManager = storageManager;
        this.parent = parent;
        this.nameResolver = nameResolver;
        this.descriptorFinder = descriptorFinder;
        this.debugName = debugName + (parent == null ? "" : ". Child of " + parent.debugName);

        for (DeserializedTypeParameterDescriptor typeParameterDescriptor : typeParameterResolver.getTypeParameters(this)) {
            typeParameterDescriptors.put(typeParameterDescriptor.getProtoId(), typeParameterDescriptor);
        }

        this.classDescriptors = storageManager.createMemoizedFunctionWithNullableValues(new Function1<Integer, ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke(Integer fqNameIndex) {
                return computeClassDescriptor(fqNameIndex);
            }
        });
    }

    /* package */ DescriptorFinder getDescriptorFinder() {
        return descriptorFinder;
    }

    @Nullable
    public JetType typeOrNull(@Nullable ProtoBuf.Type proto) {
        if (proto == null) {
            return null;
        }
        return type(proto);
    }

    @NotNull
    public JetType type(@NotNull ProtoBuf.Type proto) {
        return new DeserializedType(proto);
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
        ClassId classId = nameResolver.getClassId(fqNameIndex);
        return descriptorFinder.findClass(classId);
    }

    private List<TypeProjection> typeArguments(List<ProtoBuf.Type.Argument> protos) {
        List<TypeProjection> result = new ArrayList<TypeProjection>(protos.size());
        for (ProtoBuf.Type.Argument proto : protos) {
            result.add(typeProjection(proto));
        }
        return result;
    }

    private TypeProjection typeProjection(ProtoBuf.Type.Argument proto) {
        return new TypeProjectionImpl(
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

    @Override
    public String toString() {
        return debugName;
    }

    private class DeserializedType extends AbstractJetType {
        private final ProtoBuf.Type typeProto;
        private final NotNullLazyValue<TypeConstructor> constructor;
        private final List<TypeProjection> arguments;
        private final NotNullLazyValue<JetScope> memberScope;

        public DeserializedType(@NotNull ProtoBuf.Type proto) {
            this.typeProto = proto;
            this.arguments = typeArguments(proto.getArgumentList());

            this.constructor = storageManager.createLazyValue(new Function0<TypeConstructor>() {
                @Override
                public TypeConstructor invoke() {
                    return typeConstructor(typeProto);
                }
            });
            this.memberScope = storageManager.createLazyValue(new Function0<JetScope>() {
                @Override
                public JetScope invoke() {
                    return computeMemberScope();
                }
            });
        }

        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            return constructor.invoke();
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            return arguments;
        }

        @Override
        public boolean isNullable() {
            return typeProto.getNullable();
        }

        @NotNull
        private JetScope computeMemberScope() {
            if (isError()) {
                return ErrorUtils.createErrorScope(getConstructor().toString());
            }
            else {
                return getTypeMemberScope(getConstructor(), getArguments());
            }
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            return memberScope.invoke();
        }

        @Override
        public boolean isError() {
            ClassifierDescriptor descriptor = getConstructor().getDeclarationDescriptor();
            return descriptor != null && ErrorUtils.isError(descriptor);
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            return Collections.emptyList();
        }
    }
}
