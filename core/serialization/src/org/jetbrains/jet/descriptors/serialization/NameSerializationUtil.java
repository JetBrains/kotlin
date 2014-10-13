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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NameSerializationUtil {
    private NameSerializationUtil() {
    }

    @NotNull
    public static NameResolver deserializeNameResolver(@NotNull InputStream in) {
        try {
            ProtoBuf.StringTable simpleNames = ProtoBuf.StringTable.parseDelimitedFrom(in);
            ProtoBuf.QualifiedNameTable qualifiedNames = ProtoBuf.QualifiedNameTable.parseDelimitedFrom(in);
            return new NameResolver(simpleNames, qualifiedNames);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    public static void serializeNameResolver(@NotNull OutputStream out, @NotNull NameResolver nameResolver) {
        serializeNameTable(out, nameResolver.getStringTable(), nameResolver.getQualifiedNameTable());
    }

    public static void serializeNameTable(@NotNull OutputStream out, @NotNull NameTable nameTable) {
        serializeNameTable(out, toStringTable(nameTable), toQualifiedNameTable(nameTable));
    }

    private static void serializeNameTable(
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

    @NotNull
    public static ProtoBuf.StringTable toStringTable(@NotNull NameTable nameTable) {
        ProtoBuf.StringTable.Builder simpleNames = ProtoBuf.StringTable.newBuilder();
        for (String simpleName : nameTable.getStrings()) {
            simpleNames.addString(simpleName);
        }
        return simpleNames.build();
    }

    @NotNull
    public static ProtoBuf.QualifiedNameTable toQualifiedNameTable(@NotNull NameTable nameTable) {
        ProtoBuf.QualifiedNameTable.Builder qualifiedNames = ProtoBuf.QualifiedNameTable.newBuilder();
        for (ProtoBuf.QualifiedNameTable.QualifiedName.Builder qName : nameTable.getFqNames()) {
            qualifiedNames.addQualifiedName(qName);
        }
        return qualifiedNames.build();
    }

    @NotNull
    public static NameResolver createNameResolver(@NotNull NameTable table) {
        return new NameResolver(toStringTable(table), toQualifiedNameTable(table));
    }
}
