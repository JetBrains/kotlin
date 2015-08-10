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
import org.jetbrains.kotlin.load.kotlin.SignatureDeserializer;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.serialization.AnnotationSerializer;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.serialization.SerializerExtension;
import org.jetbrains.kotlin.serialization.StringTable;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.Arrays;

import static org.jetbrains.kotlin.codegen.AsmUtil.shortNameByAsmType;
import static org.jetbrains.kotlin.codegen.JvmSerializationBindings.*;

public class JvmSerializerExtension extends SerializerExtension {
    private final JvmSerializationBindings bindings;
    private final JetTypeMapper typeMapper;
    private final AnnotationSerializer annotationSerializer = new AnnotationSerializer();

    public JvmSerializerExtension(@NotNull JvmSerializationBindings bindings, @NotNull JetTypeMapper typeMapper) {
        this.bindings = bindings;
        this.typeMapper = typeMapper;
    }

    @Override
    public void serializeClass(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class.Builder proto, @NotNull StringTable stringTable) {
        AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(KotlinBuiltIns.FQ_NAMES.annotation);
        if (annotation != null) {
            proto.addExtension(JvmProtoBuf.classAnnotation, annotationSerializer.serializeAnnotation(annotation, stringTable));
        }
    }

    @Override
    public void serializeCallable(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull StringTable stringTable
    ) {
        saveSignature(callable, proto, stringTable);
        saveImplClassName(callable, proto, stringTable);
    }

    @Override
    public void serializeValueParameter(
            @NotNull ValueParameterDescriptor descriptor,
            @NotNull ProtoBuf.Callable.ValueParameter.Builder proto,
            @NotNull StringTable stringTable
    ) {
        Integer index = bindings.get(INDEX_FOR_VALUE_PARAMETER, descriptor);
        if (index != null) {
            proto.setExtension(JvmProtoBuf.index, index);
        }
    }

    @Override
    public void serializeType(@NotNull JetType type, @NotNull ProtoBuf.Type.Builder proto, @NotNull StringTable stringTable) {
        // TODO: don't store type annotations in our binary metadata on Java 8, use *TypeAnnotations attributes instead
        for (AnnotationDescriptor annotation : type.getAnnotations()) {
            proto.addExtension(JvmProtoBuf.typeAnnotation, annotationSerializer.serializeAnnotation(annotation, stringTable));
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

    private void saveSignature(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull StringTable stringTable
    ) {
        SignatureSerializer signatureSerializer = new SignatureSerializer(stringTable);
        if (callable instanceof FunctionDescriptor) {
            JvmProtoBuf.JvmMethodSignature signature;
            if (callable instanceof DeserializedSimpleFunctionDescriptor) {
                DeserializedSimpleFunctionDescriptor deserialized = (DeserializedSimpleFunctionDescriptor) callable;
                signature = signatureSerializer.copyMethodSignature(
                        deserialized.getProto().getExtension(JvmProtoBuf.methodSignature), deserialized.getNameResolver());
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
            Type fieldType;
            String fieldName;
            boolean isStaticInOuter;
            Method syntheticMethod;
            if (field != null) {
                fieldType = field.first;
                fieldName = field.second;
                isStaticInOuter = bindings.get(STATIC_FIELD_IN_OUTER_CLASS, property);
                syntheticMethod = null;
            }
            else {
                fieldType = null;
                fieldName = null;
                isStaticInOuter = false;
                syntheticMethod = bindings.get(SYNTHETIC_METHOD_FOR_PROPERTY, property);
            }

            JvmProtoBuf.JvmPropertySignature signature;
            if (callable instanceof DeserializedPropertyDescriptor) {
                DeserializedPropertyDescriptor deserializedCallable = (DeserializedPropertyDescriptor) callable;
                signature = signatureSerializer.copyPropertySignature(
                        deserializedCallable.getProto().getExtension(JvmProtoBuf.propertySignature),
                        deserializedCallable.getNameResolver()
                );
            }
            else {
                signature = signatureSerializer
                        .propertySignature(fieldType, fieldName, isStaticInOuter, syntheticMethod, getterMethod, setterMethod);
            }
            proto.setExtension(JvmProtoBuf.propertySignature, signature);
        }
    }

    private void saveImplClassName(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull StringTable stringTable
    ) {
        String name = bindings.get(IMPL_CLASS_NAME_FOR_CALLABLE, callable);
        if (name != null) {
            proto.setExtension(JvmProtoBuf.implClassName, stringTable.getSimpleNameIndex(Name.identifier(name)));
        }
    }

    private static class SignatureSerializer {
        private final StringTable stringTable;

        public SignatureSerializer(@NotNull StringTable stringTable) {
            this.stringTable = stringTable;
        }

        @NotNull
        public JvmProtoBuf.JvmMethodSignature copyMethodSignature(
                @NotNull JvmProtoBuf.JvmMethodSignature signature,
                @NotNull NameResolver nameResolver
        ) {
            String method = new SignatureDeserializer(nameResolver).methodSignatureString(signature);
            return methodSignature(getAsmMethod(method));
        }

        @NotNull
        public JvmProtoBuf.JvmMethodSignature methodSignature(@NotNull Method method) {
            JvmProtoBuf.JvmMethodSignature.Builder signature = JvmProtoBuf.JvmMethodSignature.newBuilder();

            signature.setName(stringTable.getStringIndex(method.getName()));

            signature.setReturnType(type(method.getReturnType()));

            for (Type type : method.getArgumentTypes()) {
                signature.addParameterType(type(type));
            }

            return signature.build();
        }

        @NotNull
        public JvmProtoBuf.JvmPropertySignature copyPropertySignature(
                @NotNull JvmProtoBuf.JvmPropertySignature signature,
                @NotNull NameResolver nameResolver
        ) {
            Type fieldType;
            String fieldName;
            boolean isStaticInOuter;
            SignatureDeserializer signatureDeserializer = new SignatureDeserializer(nameResolver);
            if (signature.hasField()) {
                JvmProtoBuf.JvmFieldSignature field = signature.getField();
                fieldType = Type.getType(signatureDeserializer.typeDescriptor(field.getType()));
                fieldName = nameResolver.getName(field.getName()).asString();
                isStaticInOuter = field.getIsStaticInOuter();
            }
            else {
                fieldType = null;
                fieldName = null;
                isStaticInOuter = false;
            }

            Method syntheticMethod = signature.hasSyntheticMethod()
                    ? getAsmMethod(signatureDeserializer.methodSignatureString(signature.getSyntheticMethod()))
                    : null;

            Method getter = signature.hasGetter() ? getAsmMethod(signatureDeserializer.methodSignatureString(signature.getGetter())) : null;
            Method setter = signature.hasSetter() ? getAsmMethod(signatureDeserializer.methodSignatureString(signature.getSetter())) : null;

            return propertySignature(fieldType, fieldName, isStaticInOuter, syntheticMethod, getter, setter);
        }

        @NotNull
        public JvmProtoBuf.JvmPropertySignature propertySignature(
                @Nullable Type fieldType,
                @Nullable String fieldName,
                boolean isStaticInOuter,
                @Nullable Method syntheticMethod,
                @Nullable Method getter,
                @Nullable Method setter
        ) {
            JvmProtoBuf.JvmPropertySignature.Builder signature = JvmProtoBuf.JvmPropertySignature.newBuilder();

            if (fieldType != null) {
                assert fieldName != null : "Field name shouldn't be null when there's a field type: " + fieldType;
                signature.setField(fieldSignature(fieldType, fieldName, isStaticInOuter));
            }

            if (syntheticMethod != null) {
                signature.setSyntheticMethod(methodSignature(syntheticMethod));
            }

            if (getter != null) {
                signature.setGetter(methodSignature(getter));
            }
            if (setter != null) {
                signature.setSetter(methodSignature(setter));
            }

            return signature.build();
        }

        @NotNull
        public JvmProtoBuf.JvmFieldSignature fieldSignature(@NotNull Type type, @NotNull String name, boolean isStaticInOuter) {
            JvmProtoBuf.JvmFieldSignature.Builder signature = JvmProtoBuf.JvmFieldSignature.newBuilder();
            signature.setName(stringTable.getStringIndex(name));
            signature.setType(type(type));
            if (isStaticInOuter) {
                signature.setIsStaticInOuter(true);
            }
            return signature.build();
        }

        @NotNull
        public JvmProtoBuf.JvmType type(@NotNull Type givenType) {
            JvmProtoBuf.JvmType.Builder builder = JvmProtoBuf.JvmType.newBuilder();

            Type type = givenType;
            if (type.getSort() == Type.ARRAY) {
                builder.setArrayDimension(type.getDimensions());
                type = type.getElementType();
            }

            if (type.getSort() == Type.OBJECT) {
                FqName fqName = internalNameToFqName(type.getInternalName());
                builder.setClassFqName(stringTable.getFqNameIndex(fqName));
            }
            else {
                builder.setPrimitiveType(JvmProtoBuf.JvmType.PrimitiveType.valueOf(type.getSort()));
            }

            return builder.build();
        }

        @NotNull
        private static FqName internalNameToFqName(@NotNull String internalName) {
            return FqName.fromSegments(Arrays.asList(internalName.split("/")));
        }
    }

    @NotNull
    private static Method getAsmMethod(@NotNull String nameAndDesc) {
        int indexOf = nameAndDesc.indexOf('(');
        return new Method(nameAndDesc.substring(0, indexOf), nameAndDesc.substring(indexOf));
    }
}
