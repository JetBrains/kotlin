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

package org.jetbrains.jet.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class LibraryUtils {
    public static final String VENDOR_JETBRAINS = "JetBrains";

    public static final String TITLE_KOTLIN_RUNTIME_AND_STDLIB = "Kotlin Compiler Runtime + StdLib";
    public static final String TITLE_KOTLIN_RUNTIME_AND_STDLIB_SOURCES = "Kotlin Compiler Runtime + StdLib Sources";
    public static final String TITLE_KOTLIN_JAVASCRIPT_STDLIB = "Kotlin JavaScript StdLib";

    private LibraryUtils() {}

    @Nullable
    public static Manifest getManifestFromJar(@NotNull File library) {
        if (!library.canRead()) return null;

        try {
            JarFile jarFile = new JarFile(library);
            try {
                return jarFile.getManifest();
            }
            finally {
                jarFile.close();
            }
        }
        catch (IOException ignored) {
            return null;
        }
    }

    @Nullable
    public static Attributes getManifestMainAttributesFromJar(@NotNull File library) {
        Manifest manifest = getManifestFromJar(library);
        return manifest != null ? manifest.getMainAttributes() : null;
    }

    private static boolean checkImplTitle(@NotNull File library, String expected) {
        Attributes attributes = getManifestMainAttributesFromJar(library);
        if (attributes == null) return false;

        String title = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        return title != null && title.equals(expected);
    }

    public static boolean isJsRuntimeLibrary(@NotNull File library) {
        return checkImplTitle(library, TITLE_KOTLIN_JAVASCRIPT_STDLIB);
    }

    public static boolean isJvmRuntimeLibrary(@NotNull File library) {
        return checkImplTitle(library, TITLE_KOTLIN_RUNTIME_AND_STDLIB);
    }
}
