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

package org.jetbrains.kotlin.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SerializationUtil {
    private SerializationUtil() {
    }

    @NotNull
    public static byte[] serializeClassData(@NotNull ClassData classData) {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            serializeNameResolver(result, classData.getNameResolver());
            classData.getClassProto().writeTo(result);
            return result.toByteArray();
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    public static byte[] serializePackageData(@NotNull PackageData packageData) {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            serializeNameResolver(result, packageData.getNameResolver());
            packageData.getPackageProto().writeTo(result);
            return result.toByteArray();
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    private static void serializeNameResolver(@NotNull OutputStream out, @NotNull NameResolver nameResolver) {
        serializeStringTable(out, nameResolver.getStringTable(), nameResolver.getQualifiedNameTable());
    }

    public static void serializeStringTable(
            @NotNull OutputStream out,
            @NotNull ProtoBuf.StringTable stringTable,
            @NotNull ProtoBuf.QualifiedNameTable qualifiedNameTable
    ) {
        try {
            stringTable.writeDelimitedTo(out);
            qualifiedNameTable.writeDelimitedTo(out);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }
}
