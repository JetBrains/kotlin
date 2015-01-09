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

public final class ClassData {
    @NotNull
    public static ClassData read(@NotNull byte[] bytes, @NotNull ExtensionRegistryLite registry) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            NameResolver nameResolver = NameSerializationUtil.deserializeNameResolver(in);
            ProtoBuf.Class classProto = ProtoBuf.Class.parseFrom(in, registry);
            return new ClassData(nameResolver, classProto);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    private final NameResolver nameResolver;

    private final ProtoBuf.Class classProto;

    public ClassData(@NotNull NameResolver nameResolver, @NotNull ProtoBuf.Class classProto) {
        this.nameResolver = nameResolver;
        this.classProto = classProto;
    }

    @NotNull
    public NameResolver getNameResolver() {
        return nameResolver;
    }

    @NotNull
    public ProtoBuf.Class getClassProto() {
        return classProto;
    }

    @NotNull
    public byte[] toBytes() {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            NameSerializationUtil.serializeNameResolver(result, nameResolver);
            classProto.writeTo(result);
            return result.toByteArray();
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }
}
