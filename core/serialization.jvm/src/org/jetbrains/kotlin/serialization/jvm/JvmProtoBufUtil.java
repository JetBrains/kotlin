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

package org.jetbrains.kotlin.serialization.jvm;

import com.google.protobuf.ExtensionRegistryLite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.serialization.ClassData;
import org.jetbrains.kotlin.serialization.PackageData;

public class JvmProtoBufUtil {
    private JvmProtoBufUtil() {
    }

    @NotNull
    public static ExtensionRegistryLite getExtensionRegistry() {
        ExtensionRegistryLite registry = ExtensionRegistryLite.newInstance();
        JvmProtoBuf.registerAllExtensions(registry);
        return registry;
    }

    @NotNull
    public static ClassData readClassDataFrom(@NotNull String[] encodedData) {
        return ClassData.read(BitEncoding.decodeBytes(encodedData), getExtensionRegistry());
    }

    @NotNull
    public static PackageData readPackageDataFrom(@NotNull String[] encodedData) {
        return readPackageDataFrom(BitEncoding.decodeBytes(encodedData));
    }

    @NotNull
    public static PackageData readPackageDataFrom(@NotNull byte[] data) {
        return PackageData.read(data, getExtensionRegistry());
    }
}
