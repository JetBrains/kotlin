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
    public static ClassData readClassDataFrom(@NotNull byte[] data) {
        return ClassData.read(data, getExtensionRegistry());
    }

    @NotNull
    public static PackageData readPackageDataFrom(@NotNull byte[] data) {
        return PackageData.read(data, getExtensionRegistry());
    }
}
