/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.serialization;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ClassBuilderMode;
import org.jetbrains.kotlin.codegen.FakeDescriptorsForReferencesKt;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.lazy.types.RawTypeImpl;
import org.jetbrains.kotlin.load.kotlin.JavaFlexibleTypeDeserializer;
import org.jetbrains.kotlin.load.kotlin.TypeSignatureMappingKt;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.serialization.*;
import org.jetbrains.kotlin.serialization.jvm.ClassMapperLite;
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf;
import org.jetbrains.kotlin.types.FlexibleType;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.List;

import static org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.*;

public class JvmSerializerExtension extends SerializerExtension {
    private final JvmSerializationBindings bindings;
    private final BindingContext codegenBinding;
    private final KotlinTypeMapper typeMapper;
    private final StringTable stringTable;
    private final AnnotationSerializer annotationSerializer;
    private final boolean useTypeTable;
    private final String moduleName;
    private final ClassBuilderMode classBuilderMode;

    public JvmSerializerExtension(@NotNull JvmSerializationBindings bindings, @NotNull GenerationState state) {
        this.bindings = bindings;
        this.codegenBinding = state.getBindingContext();
        this.typeMapper = state.getTypeMapper();
        this.stringTable = new JvmStringTable(typeMapper);
        this.annotationSerializer = new AnnotationSerializer(stringTable);
        this.useTypeTable = state.getUseTypeTableInSerializer();
        this.moduleName = state.getModuleName();
        this.classBuilderMode = state.getClassBuilderMode();
    }

    @NotNull
    @Override
    public StringTable getStringTable() {
        return stringTable;
    }

    @Override
    public boolean shouldUseTypeTable() {
        return useTypeTable;
    }

    @Override
    public void serializeClass(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class.Builder proto) {
        if (!moduleName.equals(JvmAbi.DEFAULT_MODULE_NAME)) {
            proto.setExtension(JvmProtoBuf.classModuleName, stringTable.getStringIndex(moduleName));
        }

        Type containerAsmType =
                DescriptorUtils.isInterface(descriptor) ? typeMapper.mapDefaultImpls(descriptor) : typeMapper.mapClass(descriptor);
        writeLocalProperties(proto, containerAsmType, JvmProtoBuf.classLocalVariable);
    }

    @Override
    public void serializePackage(@NotNull FqName packageFqName, @NotNull ProtoBuf.Package.Builder proto) {
        if (!moduleName.equals(JvmAbi.DEFAULT_MODULE_NAME)) {
            proto.setExtension(JvmProtoBuf.packageModuleName, stringTable.getStringIndex(moduleName));
        }
    }

    public void serializeJvmPackage(@NotNull ProtoBuf.Package.Builder proto, @NotNull Type partAsmType) {
        writeLocalProperties(proto, partAsmType, JvmProtoBuf.packageLocalVariable);
    }

    private <MessageType extends GeneratedMessageLite.ExtendableMessage<MessageType>,
            BuilderType extends GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>> void writeLocalProperties(
            @NotNull BuilderType proto,
            @NotNull Type classAsmType,
            @NotNull GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Property>> extension
    ) {
        List<VariableDescriptorWithAccessors> localVariables = codegenBinding.get(CodegenBinding.DELEGATED_PROPERTIES, classAsmType);
        if (localVariables == null) return;

        for (VariableDescriptorWithAccessors localVariable : localVariables) {
            if (localVariable instanceof LocalVariableDescriptor) {
                PropertyDescriptor propertyDescriptor =
                        FakeDescriptorsForReferencesKt.createFreeFakeLocalPropertyDescriptor((LocalVariableDescriptor) localVariable);
                DescriptorSerializer serializer = DescriptorSerializer.createForLambda(this);
                proto.addExtension(extension, serializer.propertyProto(propertyDescriptor).build());
            }
        }
    }

    @Override
    public void serializeFlexibleType(
            @NotNull FlexibleType flexibleType,
            @NotNull ProtoBuf.Type.Builder lowerProto,
            @NotNull ProtoBuf.Type.Builder upperProto
    ) {
        lowerProto.setFlexibleTypeCapabilitiesId(getStringTable().getStringIndex(JavaFlexibleTypeDeserializer.INSTANCE.getId()));

        if (flexibleType instanceof RawTypeImpl) {
            lowerProto.setExtension(JvmProtoBuf.isRaw, true);

            // we write this Extension for compatibility with old compiler
            upperProto.setExtension(JvmProtoBuf.isRaw, true);
        }
    }

    @Override
    public void serializeType(@NotNull KotlinType type, @NotNull ProtoBuf.Type.Builder proto) {
        // TODO: don't store type annotations in our binary metadata on Java 8, use *TypeAnnotations attributes instead
        for (AnnotationDescriptor annotation : type.getAnnotations()) {
            proto.addExtension(JvmProtoBuf.typeAnnotation, annotationSerializer.serializeAnnotation(annotation));
        }
    }

    @Override
    public void serializeTypeParameter(
            @NotNull TypeParameterDescriptor typeParameter, @NotNull ProtoBuf.TypeParameter.Builder proto
    ) {
        for (AnnotationDescriptor annotation : typeParameter.getAnnotations()) {
            proto.addExtension(JvmProtoBuf.typeParameterAnnotation, annotationSerializer.serializeAnnotation(annotation));
        }
    }

    @Override
    public void serializeConstructor(@NotNull ConstructorDescriptor descriptor, @NotNull ProtoBuf.Constructor.Builder proto) {
        Method method = bindings.get(METHOD_FOR_FUNCTION, descriptor);
        if (method != null) {
            JvmProtoBuf.JvmMethodSignature signature = new SignatureSerializer().methodSignature(descriptor, method);
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.constructorSignature, signature);
            }
        }
    }

    @Override
    public void serializeFunction(@NotNull FunctionDescriptor descriptor, @NotNull ProtoBuf.Function.Builder proto) {
        Method method = bindings.get(METHOD_FOR_FUNCTION, descriptor);
        if (method != null) {
            JvmProtoBuf.JvmMethodSignature signature = new SignatureSerializer().methodSignature(descriptor, method);
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.methodSignature, signature);
            }
        }
    }

    @Override
    public void serializeProperty(@NotNull PropertyDescriptor descriptor, @NotNull ProtoBuf.Property.Builder proto) {
        SignatureSerializer signatureSerializer = new SignatureSerializer();

        PropertyGetterDescriptor getter = descriptor.getGetter();
        PropertySetterDescriptor setter = descriptor.getSetter();
        Method getterMethod = getter == null ? null : bindings.get(METHOD_FOR_FUNCTION, getter);
        Method setterMethod = setter == null ? null : bindings.get(METHOD_FOR_FUNCTION, setter);

        Pair<Type, String> field = bindings.get(FIELD_FOR_PROPERTY, descriptor);
        Method syntheticMethod = bindings.get(SYNTHETIC_METHOD_FOR_PROPERTY, descriptor);

        JvmProtoBuf.JvmPropertySignature signature = signatureSerializer.propertySignature(
                descriptor,
                field != null ? field.second : null,
                field != null ? field.first.getDescriptor() : null,
                syntheticMethod != null ? signatureSerializer.methodSignature(null, syntheticMethod) : null,
                getterMethod != null ? signatureSerializer.methodSignature(null, getterMethod) : null,
                setterMethod != null ? signatureSerializer.methodSignature(null, setterMethod) : null
        );

        proto.setExtension(JvmProtoBuf.propertySignature, signature);
    }

    @Override
    public void serializeErrorType(@NotNull KotlinType type, @NotNull ProtoBuf.Type.Builder builder) {
        if (classBuilderMode == ClassBuilderMode.KAPT || classBuilderMode == ClassBuilderMode.KAPT3) {
            builder.setClassName(stringTable.getStringIndex(TypeSignatureMappingKt.NON_EXISTENT_CLASS_NAME));
            return;
        }

        super.serializeErrorType(type, builder);
    }

    private class SignatureSerializer {
        @Nullable
        public JvmProtoBuf.JvmMethodSignature methodSignature(@Nullable FunctionDescriptor descriptor, @NotNull Method method) {
            JvmProtoBuf.JvmMethodSignature.Builder builder = JvmProtoBuf.JvmMethodSignature.newBuilder();
            if (descriptor == null || !descriptor.getName().asString().equals(method.getName())) {
                builder.setName(stringTable.getStringIndex(method.getName()));
            }
            if (descriptor == null || requiresSignature(descriptor, method.getDescriptor())) {
                builder.setDesc(stringTable.getStringIndex(method.getDescriptor()));
            }
            return builder.hasName() || builder.hasDesc() ? builder.build() : null;
        }

        // We don't write those signatures which can be trivially reconstructed from already serialized data
        // TODO: make JvmStringTable implement NameResolver and use JvmProtoBufUtil#getJvmMethodSignature instead
        private boolean requiresSignature(@NotNull FunctionDescriptor descriptor, @NotNull String desc) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
            if (receiverParameter != null) {
                String receiverDesc = mapTypeDefault(receiverParameter.getValue().getType());
                if (receiverDesc == null) return true;
                sb.append(receiverDesc);
            }

            for (ValueParameterDescriptor valueParameter : descriptor.getValueParameters()) {
                String paramDesc = mapTypeDefault(valueParameter.getType());
                if (paramDesc == null) return true;
                sb.append(paramDesc);
            }

            sb.append(")");

            KotlinType returnType = descriptor.getReturnType();
            String returnTypeDesc = returnType == null ? "V" : mapTypeDefault(returnType);
            if (returnTypeDesc == null) return true;
            sb.append(returnTypeDesc);

            return !sb.toString().equals(desc);
        }

        private boolean requiresSignature(@NotNull PropertyDescriptor descriptor, @NotNull String desc) {
            return !desc.equals(mapTypeDefault(descriptor.getType()));
        }

        @Nullable
        private String mapTypeDefault(@NotNull KotlinType type) {
            ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
            if (!(classifier instanceof ClassDescriptor)) return null;
            ClassId classId = classId((ClassDescriptor) classifier);
            return classId == null ? null : ClassMapperLite.mapClass(classId);
        }

        @Nullable
        private ClassId classId(@NotNull ClassDescriptor descriptor) {
            DeclarationDescriptor container = descriptor.getContainingDeclaration();
            if (container instanceof PackageFragmentDescriptor) {
                return ClassId.topLevel(((PackageFragmentDescriptor) container).getFqName().child(descriptor.getName()));
            }
            else if (container instanceof ClassDescriptor) {
                ClassId outerClassId = classId((ClassDescriptor) container);
                return outerClassId == null ? null : outerClassId.createNestedClassId(descriptor.getName());
            }
            else {
                return null;
            }
        }

        @NotNull
        public JvmProtoBuf.JvmPropertySignature propertySignature(
                @NotNull PropertyDescriptor descriptor,
                @Nullable String fieldName,
                @Nullable String fieldDesc,
                @Nullable JvmProtoBuf.JvmMethodSignature syntheticMethod,
                @Nullable JvmProtoBuf.JvmMethodSignature getter,
                @Nullable JvmProtoBuf.JvmMethodSignature setter
        ) {
            JvmProtoBuf.JvmPropertySignature.Builder signature = JvmProtoBuf.JvmPropertySignature.newBuilder();

            if (fieldDesc != null) {
                assert fieldName != null : "Field name shouldn't be null when there's a field type: " + fieldDesc;
                signature.setField(fieldSignature(descriptor, fieldName, fieldDesc));
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
        public JvmProtoBuf.JvmFieldSignature fieldSignature(
                @NotNull PropertyDescriptor descriptor,
                @NotNull String name,
                @NotNull String desc
        ) {
            JvmProtoBuf.JvmFieldSignature.Builder builder = JvmProtoBuf.JvmFieldSignature.newBuilder();
            if (!descriptor.getName().asString().equals(name)) {
                builder.setName(stringTable.getStringIndex(name));
            }
            if (requiresSignature(descriptor, desc)) {
                builder.setDesc(stringTable.getStringIndex(desc));
            }
            return builder.build();
        }
    }
}
