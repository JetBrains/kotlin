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

package org.jetbrains.kotlin.idea.framework;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.utils.LibraryUtils;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JsLibraryStdDetectionUtil {
    private static final Logger LOG = Logger.getInstance(JsLibraryStdDetectionUtil.class);

    public static String getJsLibraryStdVersion(@NotNull List<VirtualFile> classesRoots) {
        return getJsLibraryStdVersion(classesRoots, true);
    }

    public static boolean hasJsStdlibJar(@NotNull Library library) {
        List<VirtualFile> classes = Arrays.asList(library.getFiles(OrderRootType.CLASSES));
        return getJsLibraryStdVersion(classes, false) != null;
    }

    private static String getJsLibraryStdVersion(@NotNull List<VirtualFile> classesRoots, boolean fixedJarName) {
        if (JavaRuntimeDetectionUtil.getJavaRuntimeVersion(classesRoots) != null) {
            // Prevent clashing with java runtime, in case when library collects all roots.
            return null;
        }

        VirtualFile jar = fixedJarName ? LibraryUtils.getJarFile(classesRoots, PathUtil.JS_LIB_JAR_NAME) : getJsStdLibJar(classesRoots);
        if (jar == null) return null;

        assert JsHeaderLibraryDetectionUtil.isJsHeaderLibraryDetected(classesRoots) : "StdLib should also be detected as headers library";

        return getJarAttribute(VfsUtilCore.virtualToIoFile(jar), Attributes.Name.IMPLEMENTATION_VERSION);
    }

    @Nullable
    private static VirtualFile getJsStdLibJar(@NotNull List<VirtualFile> classesRoots) {
        for (VirtualFile root : classesRoots) {
            if (root.getFileSystem().getProtocol() != StandardFileSystems.JAR_PROTOCOL) continue;

            VirtualFile jar = VfsUtilCore.getVirtualFileForJar(root);
            assert jar != null : "expected not null for root '" + root.getPath() + "'";

            if (LibraryUtils.isKotlinJavascriptStdLibrary(new File(jar.getPath()))) {
                return jar;
            }
        }

        return null;
    }

    /**
     * Returns attribute value from a manifest main section,
     * or null if missing or a file does not contain a manifest.
     *
     * Copied from Idea 14 JarUtil.java
     */
    @Nullable
    public static String getJarAttribute(@NotNull File file, @NotNull Attributes.Name attribute) {
        return getJarAttributeImpl(file, null, attribute);
    }

    /**
     * Copied from Idea 14 JarUtil.java
     */
    private static String getJarAttributeImpl(@NotNull File file, @Nullable String entryName, @NotNull Attributes.Name attribute) {
        if (file.canRead()) {
            try {
                JarFile jarFile = new JarFile(file);
                try {
                    Manifest manifest = jarFile.getManifest();
                    if (manifest != null) {
                        Attributes attributes = entryName != null ? manifest.getAttributes(entryName) : manifest.getMainAttributes();
                        return attributes.getValue(attribute);
                    }
                }
                finally {
                    jarFile.close();
                }
            }
            catch (IOException e) {
                LOG.debug(e);
            }
        }

        return null;
    }
}
