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

import static org.jetbrains.jet.descriptors.serialization.JavaProtoBuf.javaSignature;

public class JavaProtoBufUtil {
    private JavaProtoBufUtil() {
    }

    @Nullable
    public static String loadJavaSignature(@NotNull ProtoBuf.Callable callable) {
        return callable.hasExtension(javaSignature) ? callable.getExtension(javaSignature) : null;
    }

    public static void saveJavaSignature(@NotNull ProtoBuf.Callable.Builder callable, @NotNull String signature) {
        callable.setExtension(javaSignature, signature);
    }

    @NotNull
    private static ExtensionRegistryLite getExtensionRegistry() {
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
