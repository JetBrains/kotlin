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
    public static String loadMethodSignature(@NotNull ProtoBuf.Callable proto, @NotNull NameResolver nameResolver) {
        if (!proto.hasExtension(JavaProtoBuf.methodSignature)) return null;
        JavaProtoBuf.JavaMethodSignature signature = proto.getExtension(JavaProtoBuf.methodSignature);
        return new Deserializer(nameResolver).methodSignature(signature).toString();
    }

    public static void saveMethodSignature(
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull Method method,
            @NotNull NameTable nameTable
    ) {
        proto.setExtension(JavaProtoBuf.methodSignature, new Serializer(nameTable).methodSignature(method));
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
        private JavaProtoBuf.JavaType type(@NotNull Type givenType) {
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
        return ClassData.read(decodeBytes(data), getExtensionRegistry());
    }

    @NotNull
    public static PackageData readPackageDataFrom(@NotNull String[] data) {
        return PackageData.read(decodeBytes(data), getExtensionRegistry());
    }

    /**
     * Converts a byte array of serialized data to an array of {@code String} satisfying JVM annotation value argument restrictions:
     * <ol>
     *     <li>Each string's length should be no more than 65535</li>
     *     <li>UTF-8 representation of each string cannot contain byte 0x0 or bytes in the range 0xf0..0xff</li>
     * </ol>
     */
    @NotNull
    public static String[] encodeBytes(@NotNull byte[] data) {
        // Each byte of data is split into two 4-bit parts ('lo' and 'hi'), then lo + 1 and hi + 1 are appended to the string. Hence, each
        // byte of the string is in the range 0x01..0x10 and this guarantees there's no byte 0x0 and no bytes in the range 0xf0..0xff
        // TODO: use Scala's approach instead (break data into chunks of 7 bits)
        int m = 32766;
        assert 2 * m <= 65535 : m;

        int n = data.length;
        String[] result = new String[(n + m - 1) / m];
        for (int offset = 0, resultIndex = 0; offset < n; offset += m, resultIndex++) {
            int length = Math.min(n - offset, m);
            byte[] a = new byte[length * 2];
            for (int i = 0; i < length; i++) {
                int lo = data[offset + i] & 0x0f;
                int hi = (data[offset + i] & 0xf0) >>> 4;
                a[2 * i] = (byte) (lo + 1);
                a[2 * i + 1] = (byte) (hi + 1);
            }
            result[resultIndex] = new String(a);
        }
        return result;
    }

    /**
     * Converts encoded array of {@code String} obtained by {@link JavaProtoBufUtil#encodeBytes(byte[])} back to a byte array.
     */
    @NotNull
    public static byte[] decodeBytes(@NotNull String[] data) {
        int length = 0;
        for (String s : data) {
            assert s.length() % 2 == 0 : s.length();
            length += s.length() / 2;
        }
        byte[] result = new byte[length];

        int p = 0;
        for (String s : data) {
            for (int i = 0, n = s.length(); i < n; i += 2) {
                int lo = s.charAt(i) - 1;
                int hi = s.charAt(i + 1) - 1;
                assert 0 <= lo && lo < 0xf0 : lo;
                assert 0 <= hi && hi < 0xf0 : hi;
                result[p++] = (byte) (lo + (hi << 4));
            }
        }

        assert p == length;

        return result;
    }
}
