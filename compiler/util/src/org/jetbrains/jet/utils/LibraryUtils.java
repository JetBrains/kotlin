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
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class LibraryUtils {
    private static final Logger LOG = Logger.getInstance(LibraryUtils.class);

    public static final String TITLE_KOTLIN_JVM_RUNTIME_AND_STDLIB;
    public static final String TITLE_KOTLIN_JAVASCRIPT_STDLIB;
    public static final String TITLE_KOTLIN_JAVASCRIPT_LIB;
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
    private static final Attributes.Name KOTLIN_JS_MODULE_NAME = new Attributes.Name("Kotlin-JS-Module-Name");

    static {
        String jsStdLib = "";
        String jsLib = "";
        String jvmStdLib = "";

        InputStream manifestProperties = LibraryUtils.class.getResourceAsStream("/manifest.properties");
        if (manifestProperties != null) {
            try {
                Properties properties = new Properties();
                properties.load(manifestProperties);
                jvmStdLib = properties.getProperty("manifest.impl.title.kotlin.jvm.runtime");
                jsStdLib = properties.getProperty("manifest.impl.title.kotlin.javascript.stdlib");
                jsLib = properties.getProperty("manifest.spec.title.kotlin.javascript.lib");
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
        TITLE_KOTLIN_JAVASCRIPT_LIB = jsLib;
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
    public static Manifest getManifestFromDirectory(@NotNull File library) {
        if (!library.canRead() || !library.isDirectory()) return null;

        try {
            InputStream inputStream = new FileInputStream(new File(library, MANIFEST_PATH));
            try {
                return new Manifest(inputStream);
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException ignored) {
            LOG.warn("IOException " + ignored);
            return null;
        }
    }

    private static Manifest getManifestFromJarOrDirectory(@NotNull File library) {
        return library.isDirectory() ? getManifestFromDirectory(library) : getManifestFromJar(library);
    }

    @Nullable
    public static Attributes getManifestMainAttributesFromJarOrDirectory(@NotNull File library) {
        Manifest manifest = getManifestFromJarOrDirectory(library);
        return manifest != null ? manifest.getMainAttributes() : null;
    }

    private static boolean checkImplTitle(@NotNull File library, String expected) {
        Attributes attributes = getManifestMainAttributesFromJarOrDirectory(library);
        if (attributes == null) return false;

        String title = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        return title != null && title.equals(expected);
    }

    private static boolean checkSpecTitle(@NotNull File library, String expected) {
        Attributes attributes = getManifestMainAttributesFromJarOrDirectory(library);
        if (attributes == null) return false;

        String title = attributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
        return title != null && title.equals(expected);
    }

    @Nullable
    public static String getKotlinJsModuleName(@NotNull File library) {
        Attributes attributes = getManifestMainAttributesFromJarOrDirectory(library);
        return attributes != null ? attributes.getValue(KOTLIN_JS_MODULE_NAME) : null;
    }

    public static boolean isKotlinJavascriptLibrary(@NotNull File library) {
        return checkSpecTitle(library, TITLE_KOTLIN_JAVASCRIPT_LIB);
    }

    public static boolean isKotlinJavascriptStdLibrary(@NotNull File library) {
        return checkImplTitle(library, TITLE_KOTLIN_JAVASCRIPT_STDLIB);
    }

    public static boolean isJvmRuntimeLibrary(@NotNull File library) {
        return checkImplTitle(library, TITLE_KOTLIN_JVM_RUNTIME_AND_STDLIB);
    }

    public static VirtualFile getJarFile(@NotNull List<VirtualFile> classesRoots, @NotNull String jarName) {
        for (VirtualFile root : classesRoots) {
            if (root.getName().equals(jarName)) {
                return root;
            }
        }

        return null;
    }
}
