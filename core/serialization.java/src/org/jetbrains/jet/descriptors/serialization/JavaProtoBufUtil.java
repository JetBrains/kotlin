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

import com.google.protobuf.ExtensionRegistryLite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Arrays;

import static org.jetbrains.asm4.Type.*;

public class JavaProtoBufUtil {
    private JavaProtoBufUtil() {
    }

    @Nullable
    public static Method loadMethodSignature(@NotNull ProtoBuf.Callable proto, @NotNull NameResolver nameResolver) {
        if (!proto.hasExtension(JavaProtoBuf.methodSignature)) return null;
        JavaProtoBuf.JavaMethodSignature signature = proto.getExtension(JavaProtoBuf.methodSignature);
        return new Deserializer(nameResolver).methodSignature(signature);
    }

    @Nullable
    public static Method loadPropertyGetterSignature(@NotNull ProtoBuf.Callable proto, @NotNull NameResolver nameResolver) {
        if (!proto.hasExtension(JavaProtoBuf.propertySignature)) return null;
        JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);
        return new Deserializer(nameResolver).methodSignature(propertySignature.getGetter());
    }

    @Nullable
    public static Method loadPropertySetterSignature(@NotNull ProtoBuf.Callable proto, @NotNull NameResolver nameResolver) {
        if (!proto.hasExtension(JavaProtoBuf.propertySignature)) return null;
        JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);
        return new Deserializer(nameResolver).methodSignature(propertySignature.getSetter());
    }

    public static class PropertyData {
        private final Type fieldType;
        private final String fieldName;
        private final String syntheticMethodName;

        public PropertyData(@Nullable Type fieldType, @Nullable String fieldName, @Nullable String syntheticMethodName) {
            this.fieldType = fieldType;
            this.fieldName = fieldName;
            this.syntheticMethodName = syntheticMethodName;
        }

        @Nullable
        public Type getFieldType() {
            return fieldType;
        }

        @Nullable
        public String getFieldName() {
            return fieldName;
        }

        @Nullable
        public String getSyntheticMethodName() {
            return syntheticMethodName;
        }

        @Override
        public String toString() {
            return fieldName != null ? "Field " + fieldName + " " + fieldType : "Synthetic method " + syntheticMethodName;
        }
    }

    @Nullable
    public static PropertyData loadPropertyData(@NotNull ProtoBuf.Callable proto, @NotNull NameResolver nameResolver) {
        if (!proto.hasExtension(JavaProtoBuf.propertySignature)) return null;
        JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);

        if (propertySignature.hasField()) {
            JavaProtoBuf.JavaFieldSignature field = propertySignature.getField();
            Type type = new Deserializer(nameResolver).type(field.getType());
            Name name = nameResolver.getName(field.getName());
            return new PropertyData(type, name.asString(), null);
        }
        else if (propertySignature.hasSyntheticMethodName()) {
            Name name = nameResolver.getName(propertySignature.getSyntheticMethodName());
            return new PropertyData(null, null, name.asString());
        }
        else {
            return null;
        }
    }

    @Nullable
    public static Name loadSrcClassName(@NotNull ProtoBuf.Callable proto, @NotNull NameResolver nameResolver) {
        if (!proto.hasExtension(JavaProtoBuf.srcClassName)) return null;
        return nameResolver.getName(proto.getExtension(JavaProtoBuf.srcClassName));
    }

    public static boolean isStaticFieldInOuter(@NotNull ProtoBuf.Callable proto) {
        if (!proto.hasExtension(JavaProtoBuf.propertySignature)) return false;
        JavaProtoBuf.JavaPropertySignature propertySignature = proto.getExtension(JavaProtoBuf.propertySignature);
        return propertySignature.hasField() && propertySignature.getField().getIsStaticInOuter();
    }

    public static void saveMethodSignature(@NotNull ProtoBuf.Callable.Builder proto, @NotNull Method method, @NotNull NameTable nameTable) {
        proto.setExtension(JavaProtoBuf.methodSignature, new Serializer(nameTable).methodSignature(method));
    }

    public static void savePropertySignature(
            @NotNull ProtoBuf.Callable.Builder proto,
            @Nullable Type fieldType,
            @Nullable String fieldName,
            boolean isStaticInOuter,
            @Nullable String syntheticMethodName,
            @Nullable Method getter,
            @Nullable Method setter,
            @NotNull NameTable nameTable
    ) {
        proto.setExtension(JavaProtoBuf.propertySignature,
                new Serializer(nameTable).propertySignature(fieldType, fieldName, isStaticInOuter, syntheticMethodName, getter, setter));
    }

    public static void saveSrcClassName(
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull Name name,
            @NotNull NameTable nameTable
    ) {
        proto.setExtension(JavaProtoBuf.srcClassName, nameTable.getSimpleNameIndex(name));
    }

    private static class Serializer {
        private final NameTable nameTable;

        public Serializer(@NotNull NameTable nameTable) {
            this.nameTable = nameTable;
        }

        @NotNull
        public JavaProtoBuf.JavaMethodSignature methodSignature(@NotNull Method method) {
            JavaProtoBuf.JavaMethodSignature.Builder signature = JavaProtoBuf.JavaMethodSignature.newBuilder();

            signature.setName(nameTable.getSimpleNameIndex(Name.guess(method.getName())));

            signature.setReturnType(type(method.getReturnType()));

            for (Type type : method.getArgumentTypes()) {
                signature.addParameterType(type(type));
            }

            return signature.build();
        }

        @NotNull
        public JavaProtoBuf.JavaPropertySignature propertySignature(
                @Nullable Type fieldType,
                @Nullable String fieldName,
                boolean isStaticInOuter,
                @Nullable String syntheticMethodName,
                @Nullable Method getter,
                @Nullable Method setter
        ) {
            JavaProtoBuf.JavaPropertySignature.Builder signature = JavaProtoBuf.JavaPropertySignature.newBuilder();

            if (fieldType != null) {
                assert fieldName != null : "Field name shouldn't be null when there's a field type: " + fieldType;
                signature.setField(fieldSignature(fieldType, fieldName, isStaticInOuter));
            }

            if (syntheticMethodName != null) {
                signature.setSyntheticMethodName(nameTable.getSimpleNameIndex(Name.guess(syntheticMethodName)));
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
        public JavaProtoBuf.JavaFieldSignature fieldSignature(@NotNull Type type, @NotNull String name, boolean isStaticInOuter) {
            JavaProtoBuf.JavaFieldSignature.Builder signature = JavaProtoBuf.JavaFieldSignature.newBuilder();
            signature.setName(nameTable.getSimpleNameIndex(Name.guess(name)));
            signature.setType(type(type));
            if (isStaticInOuter) {
                signature.setIsStaticInOuter(true);
            }
            return signature.build();
        }

        @NotNull
        public JavaProtoBuf.JavaType type(@NotNull Type givenType) {
            JavaProtoBuf.JavaType.Builder builder = JavaProtoBuf.JavaType.newBuilder();

            int arrayDimension = 0;
            Type type = givenType;
            while (type.getSort() == Type.ARRAY) {
                arrayDimension++;
                type = type.getElementType();
            }
            if (arrayDimension != 0) {
                builder.setArrayDimension(arrayDimension);
            }

            if (type.getSort() == Type.OBJECT) {
                FqName fqName = internalNameToFqName(type.getInternalName());
                builder.setClassFqName(nameTable.getFqNameIndex(fqName));
            }
            else {
                builder.setPrimitiveType(JavaProtoBuf.JavaType.PrimitiveType.valueOf(type.getSort()));
            }

            return builder.build();
        }

        @NotNull
        private static FqName internalNameToFqName(@NotNull String internalName) {
            return FqName.fromSegments(Arrays.asList(internalName.split("/")));
        }
    }

    private static class Deserializer {
        // These types are ordered according to their sorts, this is significant for deserialization
        private static final Type[] PRIMITIVE_TYPES = new Type[]
                { VOID_TYPE, BOOLEAN_TYPE, CHAR_TYPE, BYTE_TYPE, SHORT_TYPE, INT_TYPE, FLOAT_TYPE, LONG_TYPE, DOUBLE_TYPE };

        private final NameResolver nameResolver;

        public Deserializer(@NotNull NameResolver nameResolver) {
            this.nameResolver = nameResolver;
        }

        @NotNull
        public Method methodSignature(@NotNull JavaProtoBuf.JavaMethodSignature signature) {
            String name = nameResolver.getName(signature.getName()).asString();

            Type returnType = type(signature.getReturnType());

            int parameters = signature.getParameterTypeCount();
            Type[] parameterTypes = new Type[parameters];
            for (int i = 0; i < parameters; i++) {
                parameterTypes[i] = type(signature.getParameterType(i));
            }

            return new Method(name, returnType, parameterTypes);
        }

        @NotNull
        private Type type(@NotNull JavaProtoBuf.JavaType type) {
            Type result;
            if (type.hasPrimitiveType()) {
                result = PRIMITIVE_TYPES[type.getPrimitiveType().ordinal()];
            }
            else {
                result = Type.getObjectType(fqNameToInternalName(nameResolver.getFqName(type.getClassFqName())));
            }

            StringBuilder brackets = new StringBuilder(type.getArrayDimension());
            for (int i = 0; i < type.getArrayDimension(); i++) {
                brackets.append('[');
            }

            return Type.getType(brackets + result.getDescriptor());
        }

        @NotNull
        private static String fqNameToInternalName(@NotNull FqName fqName) {
            return fqName.asString().replace('.', '/');
        }
    }


    @NotNull
    public static ExtensionRegistryLite getExtensionRegistry() {
        ExtensionRegistryLite registry = ExtensionRegistryLite.newInstance();
        JavaProtoBuf.registerAllExtensions(registry);
        return registry;
    }

    @NotNull
    public static ClassData readClassDataFrom(@NotNull String[] data) {
        return ClassData.read(BitEncoding.decodeBytes(data), getExtensionRegistry());
    }

    @NotNull
    public static PackageData readPackageDataFrom(@NotNull String[] data) {
        return PackageData.read(BitEncoding.decodeBytes(data), getExtensionRegistry());
    }
}
