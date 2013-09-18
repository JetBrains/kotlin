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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Arrays;

public class JavaSerializerExtension extends SerializerExtension {
    private final MemberMap memberMap;

    public JavaSerializerExtension(@NotNull MemberMap memberMap) {
        this.memberMap = memberMap;
    }

    @Override
    public void serializeCallable(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        saveSignature(callable, proto, nameTable);
        saveSrcClassName(callable, proto, nameTable);
    }

    private void saveSignature(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        if (callable instanceof FunctionDescriptor) {
            Method method = memberMap.getMethodOfDescriptor((FunctionDescriptor) callable);
            if (method != null) {
                proto.setExtension(JavaProtoBuf.methodSignature, new SignatureSerializer(nameTable).methodSignature(method));
            }
        }
        else if (callable instanceof PropertyDescriptor) {
            PropertyDescriptor property = (PropertyDescriptor) callable;

            PropertyGetterDescriptor getter = property.getGetter();
            PropertySetterDescriptor setter = property.getSetter();
            Method getterMethod = getter == null ? null : memberMap.getMethodOfDescriptor(getter);
            Method setterMethod = setter == null ? null : memberMap.getMethodOfDescriptor(setter);

            Pair<Type, String> field = memberMap.getFieldOfProperty(property);
            Type fieldType;
            String fieldName;
            boolean isStaticInOuter;
            String syntheticMethodName;
            if (field != null) {
                fieldType = field.first;
                fieldName = field.second;
                isStaticInOuter = memberMap.isStaticFieldInOuterClass(property);
                syntheticMethodName = null;
            }
            else {
                fieldType = null;
                fieldName = null;
                isStaticInOuter = false;
                syntheticMethodName = memberMap.getSyntheticMethodNameOfProperty(property);
            }

            JavaProtoBuf.JavaPropertySignature signature = new SignatureSerializer(nameTable)
                            .propertySignature(fieldType, fieldName, isStaticInOuter, syntheticMethodName, getterMethod, setterMethod);
            proto.setExtension(JavaProtoBuf.propertySignature, signature);
        }
    }

    private void saveSrcClassName(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        Name name = memberMap.getSrcClassNameOfCallable(callable);
        if (name != null) {
            proto.setExtension(JavaProtoBuf.srcClassName, nameTable.getSimpleNameIndex(name));
        }
    }

    private static class SignatureSerializer {
        private final NameTable nameTable;

        public SignatureSerializer(@NotNull NameTable nameTable) {
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
}
