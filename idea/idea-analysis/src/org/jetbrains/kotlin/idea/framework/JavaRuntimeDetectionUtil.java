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
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.utils.PathUtil;

import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;

public class JavaRuntimeDetectionUtil {
    public static String getJavaRuntimeVersion(@NotNull Library library) {
        return getJavaRuntimeVersion(Arrays.asList(library.getFiles(OrderRootType.CLASSES)));
    }

    public static String getJavaRuntimeVersion(@NotNull List<VirtualFile> classesRoots) {
        VirtualFile stdJar = getRuntimeJar(classesRoots);
        if (stdJar != null) {
            return JarUtil.getJarAttribute(VfsUtilCore.virtualToIoFile(stdJar), Attributes.Name.IMPLEMENTATION_VERSION);
        }

        return null;
    }

    @Nullable
    public static VirtualFile getRuntimeJar(@NotNull List<VirtualFile> classesRoots) {
        for (VirtualFile root : classesRoots) {
            if (PathUtil.KOTLIN_RUNTIME_JAR_PATTERN.matcher(root.getName()).matches()) {
                return root;
            }
        }
        return null;
    }
}
