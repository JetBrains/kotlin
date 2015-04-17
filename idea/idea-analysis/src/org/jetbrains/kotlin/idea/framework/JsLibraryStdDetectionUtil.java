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

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.JarUtil;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.utils.LibraryUtils;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;

public class JsLibraryStdDetectionUtil {

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

        assert KotlinJavaScriptLibraryDetectionUtil.isKotlinJavaScriptLibrary(classesRoots) : "StdLib should also be detected as Kotlin/Javascript library";

        return JarUtil.getJarAttribute(VfsUtilCore.virtualToIoFile(jar), Attributes.Name.IMPLEMENTATION_VERSION);
    }

    @Nullable
    private static VirtualFile getJsStdLibJar(@NotNull List<VirtualFile> classesRoots) {
        for (VirtualFile root : classesRoots) {
            if (root.getFileSystem().getProtocol() != StandardFileSystems.JAR_PROTOCOL) continue;

            VirtualFile jar = VfsUtilCore.getVirtualFileForJar(root);
            if (jar != null && LibraryUtils.isKotlinJavascriptStdLibrary(new File(jar.getPath()))) {
                return jar;
            }
        }

        return null;
    }
}
