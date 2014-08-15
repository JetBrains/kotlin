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

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class LibraryUtils {
    private static final Logger LOG = Logger.getInstance(LibraryUtils.class);

    public static final String TITLE_KOTLIN_JVM_RUNTIME_AND_STDLIB;
    public static final String TITLE_KOTLIN_JAVASCRIPT_STDLIB;

    static {
        String jsStdLib = "";
        String jvmStdLib = "";

        InputStream manifestProperties = LibraryUtils.class.getResourceAsStream("/manifest.properties");
        if (manifestProperties != null) {
            try {
                Properties properties = new Properties();
                properties.load(manifestProperties);
                jvmStdLib = properties.getProperty("manifest.impl.title.kotlin.jvm.runtime");
                jsStdLib = properties.getProperty("manifest.impl.title.kotlin.javascript.stdlib");
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
        else {
            LOG.error("Resource 'manifest.properties' not found.");
        }

        TITLE_KOTLIN_JVM_RUNTIME_AND_STDLIB = jvmStdLib;
        TITLE_KOTLIN_JAVASCRIPT_STDLIB = jsStdLib;
    }

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
        return checkImplTitle(library, TITLE_KOTLIN_JVM_RUNTIME_AND_STDLIB);
    }
}
