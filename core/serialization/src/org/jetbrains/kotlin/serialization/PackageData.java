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

import com.google.protobuf.ExtensionRegistryLite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class PackageData {
    @NotNull
    public static PackageData read(@NotNull byte[] bytes, @NotNull ExtensionRegistryLite registry) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            NameResolver nameResolver = NameSerializationUtil.deserializeNameResolver(in);
            ProtoBuf.Package packageProto = ProtoBuf.Package.parseFrom(in, registry);
            return new PackageData(nameResolver, packageProto);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    private final NameResolver nameResolver;

    private final ProtoBuf.Package packageProto;

    public PackageData(@NotNull NameResolver nameResolver, @NotNull ProtoBuf.Package packageProto) {
        this.nameResolver = nameResolver;
        this.packageProto = packageProto;
    }

    @NotNull
    public NameResolver getNameResolver() {
        return nameResolver;
    }

    @NotNull
    public ProtoBuf.Package getPackageProto() {
        return packageProto;
    }

    @NotNull
    public byte[] toBytes() {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            NameSerializationUtil.serializeNameResolver(result, nameResolver);
            packageProto.writeTo(result);
            return result.toByteArray();
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }
}
