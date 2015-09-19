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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.load.java.lazy.types.RawTypeCapabilities;
import org.jetbrains.kotlin.serialization.*;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

import static org.jetbrains.kotlin.codegen.AsmUtil.shortNameByAsmType;
import static org.jetbrains.kotlin.codegen.JvmSerializationBindings.*;

public class JvmSerializerExtension extends SerializerExtension {
    private final JvmSerializationBindings bindings;
    private final JetTypeMapper typeMapper;
    private final StringTableImpl stringTable = new StringTableImpl(this);
    private final AnnotationSerializer annotationSerializer = new AnnotationSerializer(stringTable);

    public JvmSerializerExtension(@NotNull JvmSerializationBindings bindings, @NotNull JetTypeMapper typeMapper) {
        this.bindings = bindings;
        this.typeMapper = typeMapper;
    }

    @NotNull
    @Override
    public StringTable getStringTable() {
        return stringTable;
    }

    @Override
    public void serializeClass(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class.Builder proto) {
        AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(KotlinBuiltIns.FQ_NAMES.annotation);
        if (annotation != null) {
            proto.addExtension(JvmProtoBuf.classAnnotation, annotationSerializer.serializeAnnotation(annotation));
        }
    }

    @Override
    public void serializeCallable(@NotNull CallableMemberDescriptor callable, @NotNull ProtoBuf.Callable.Builder proto) {
        saveSignature(callable, proto);
        saveImplClassName(callable, proto);
    }

    @Override
    public void serializeValueParameter(
            @NotNull ValueParameterDescriptor descriptor,
            @NotNull ProtoBuf.Callable.ValueParameter.Builder proto
    ) {
        Integer index = bindings.get(INDEX_FOR_VALUE_PARAMETER, descriptor);
        if (index != null) {
            proto.setExtension(JvmProtoBuf.index, index);
        }
    }

    @Override
    public void serializeType(@NotNull JetType type, @NotNull ProtoBuf.Type.Builder proto) {
        // TODO: don't store type annotations in our binary metadata on Java 8, use *TypeAnnotations attributes instead
        for (AnnotationDescriptor annotation : type.getAnnotations()) {
            proto.addExtension(JvmProtoBuf.typeAnnotation, annotationSerializer.serializeAnnotation(annotation));
        }

        if (type.getCapabilities() instanceof RawTypeCapabilities) {
            proto.setExtension(JvmProtoBuf.isRaw, true);
        }
    }

    @Override
    @NotNull
    public String getLocalClassName(@NotNull ClassDescriptor descriptor) {
        return shortNameByAsmType(typeMapper.mapClass(descriptor));
    }

    private void saveSignature(@NotNull CallableMemberDescriptor callable, @NotNull ProtoBuf.Callable.Builder proto) {
        SignatureSerializer signatureSerializer = new SignatureSerializer();
        if (callable instanceof FunctionDescriptor) {
            JvmProtoBuf.JvmMethodSignature signature;
            if (callable instanceof DeserializedSimpleFunctionDescriptor) {
                DeserializedSimpleFunctionDescriptor deserialized = (DeserializedSimpleFunctionDescriptor) callable;
                signature = signatureSerializer.copyMethodSignature(
                        deserialized.getProto().getExtension(JvmProtoBuf.methodSignature), deserialized.getNameResolver()
                );
            }
            else {
                Method method = bindings.get(METHOD_FOR_FUNCTION, (FunctionDescriptor) callable);
                signature = method != null ? signatureSerializer.methodSignature(method) : null;
            }
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.methodSignature, signature);
            }
        }
        else if (callable instanceof PropertyDescriptor) {
            PropertyDescriptor property = (PropertyDescriptor) callable;

            PropertyGetterDescriptor getter = property.getGetter();
            PropertySetterDescriptor setter = property.getSetter();
            Method getterMethod = getter == null ? null : bindings.get(METHOD_FOR_FUNCTION, getter);
            Method setterMethod = setter == null ? null : bindings.get(METHOD_FOR_FUNCTION, setter);

            Pair<Type, String> field = bindings.get(FIELD_FOR_PROPERTY, property);
            String fieldName;
            String fieldDesc;
            boolean isStaticInOuter;
            if (field != null) {
                fieldName = field.second;
                fieldDesc = field.first.getDescriptor();
                isStaticInOuter = bindings.get(STATIC_FIELD_IN_OUTER_CLASS, property);
            }
            else {
                fieldName = null;
                fieldDesc = null;
                isStaticInOuter = false;
            }

            Method syntheticMethod = bindings.get(SYNTHETIC_METHOD_FOR_PROPERTY, property);

            JvmProtoBuf.JvmPropertySignature signature;
            if (callable instanceof DeserializedPropertyDescriptor) {
                DeserializedPropertyDescriptor deserializedCallable = (DeserializedPropertyDescriptor) callable;
                signature = signatureSerializer.copyPropertySignature(
                        deserializedCallable.getProto().getExtension(JvmProtoBuf.propertySignature),
                        deserializedCallable.getNameResolver()
                );
            }
            else {
                signature = signatureSerializer.propertySignature(
                        fieldName, fieldDesc, isStaticInOuter,
                        syntheticMethod != null ? signatureSerializer.methodSignature(syntheticMethod) : null,
                        getterMethod != null ? signatureSerializer.methodSignature(getterMethod) : null,
                        setterMethod != null ? signatureSerializer.methodSignature(setterMethod) : null
                );
            }
            proto.setExtension(JvmProtoBuf.propertySignature, signature);
        }
    }

    private void saveImplClassName(@NotNull CallableMemberDescriptor callable, @NotNull ProtoBuf.Callable.Builder proto) {
        String name = bindings.get(IMPL_CLASS_NAME_FOR_CALLABLE, callable);
        if (name != null) {
            proto.setExtension(JvmProtoBuf.implClassName, stringTable.getStringIndex(name));
        }
    }

    private class SignatureSerializer {
        @NotNull
        public JvmProtoBuf.JvmMethodSignature copyMethodSignature(
                @NotNull JvmProtoBuf.JvmMethodSignature signature,
                @NotNull NameResolver nameResolver
        ) {
            return methodSignature(new Method(
                    nameResolver.getString(signature.getName()),
                    nameResolver.getString(signature.getDesc())
            ));
        }

        @NotNull
        public JvmProtoBuf.JvmMethodSignature methodSignature(@NotNull Method method) {
            return JvmProtoBuf.JvmMethodSignature.newBuilder()
                    .setName(stringTable.getStringIndex(method.getName()))
                    .setDesc(stringTable.getStringIndex(method.getDescriptor()))
                    .build();
        }

        @NotNull
        public JvmProtoBuf.JvmPropertySignature copyPropertySignature(
                @NotNull JvmProtoBuf.JvmPropertySignature signature,
                @NotNull NameResolver nameResolver
        ) {
            String fieldName;
            String fieldDesc;
            boolean isStaticInOuter;
            if (signature.hasField()) {
                JvmProtoBuf.JvmFieldSignature field = signature.getField();
                fieldName = nameResolver.getString(field.getName());
                fieldDesc = nameResolver.getString(field.getDesc());
                isStaticInOuter = field.getIsStaticInOuter();
            }
            else {
                fieldName = null;
                fieldDesc = null;
                isStaticInOuter = false;
            }

            return propertySignature(
                    fieldName, fieldDesc, isStaticInOuter,
                    signature.hasSyntheticMethod() ? copyMethodSignature(signature.getSyntheticMethod(), nameResolver) : null,
                    signature.hasGetter() ? copyMethodSignature(signature.getGetter(), nameResolver) : null,
                    signature.hasSetter() ? copyMethodSignature(signature.getSetter(), nameResolver) : null
            );
        }

        @NotNull
        public JvmProtoBuf.JvmPropertySignature propertySignature(
                @Nullable String fieldName,
                @Nullable String fieldDesc,
                boolean isStaticInOuter,
                @Nullable JvmProtoBuf.JvmMethodSignature syntheticMethod,
                @Nullable JvmProtoBuf.JvmMethodSignature getter,
                @Nullable JvmProtoBuf.JvmMethodSignature setter
        ) {
            JvmProtoBuf.JvmPropertySignature.Builder signature = JvmProtoBuf.JvmPropertySignature.newBuilder();

            if (fieldDesc != null) {
                assert fieldName != null : "Field name shouldn't be null when there's a field type: " + fieldDesc;
                signature.setField(fieldSignature(fieldName, fieldDesc, isStaticInOuter));
            }

            if (syntheticMethod != null) {
                signature.setSyntheticMethod(syntheticMethod);
            }

            if (getter != null) {
                signature.setGetter(getter);
            }
            if (setter != null) {
                signature.setSetter(setter);
            }

            return signature.build();
        }

        @NotNull
        public JvmProtoBuf.JvmFieldSignature fieldSignature(@NotNull String name, @NotNull String desc, boolean isStaticInOuter) {
            JvmProtoBuf.JvmFieldSignature.Builder builder = JvmProtoBuf.JvmFieldSignature.newBuilder()
                    .setName(stringTable.getStringIndex(name))
                    .setDesc(stringTable.getStringIndex(desc));
            if (isStaticInOuter) {
                builder.setIsStaticInOuter(true);
            }
            return builder.build();
        }
    }
}
