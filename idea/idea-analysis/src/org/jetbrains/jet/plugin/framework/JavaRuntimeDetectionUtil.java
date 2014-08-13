/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.framework;

import com.intellij.openapi.util.io.JarUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.PathUtil;

import java.util.List;
import java.util.jar.Attributes;

public class JavaRuntimeDetectionUtil {
    public static String getJavaRuntimeVersion(@NotNull List<VirtualFile> classesRoots) {
        VirtualFile stdJar = getRuntimeJar(classesRoots);
        if (stdJar != null) {
            return JarUtil.getJarAttribute(VfsUtilCore.virtualToIoFile(stdJar), Attributes.Name.IMPLEMENTATION_VERSION);
        }

        return null;
    }

    @Nullable
    public static VirtualFile getRuntimeJar(@NotNull List<VirtualFile> classesRoots) {
        return getJarFile(classesRoots, PathUtil.KOTLIN_JAVA_RUNTIME_JAR);
    }

    static VirtualFile getJarFile(@NotNull List<VirtualFile> classesRoots, @NotNull String jarName) {
        for (VirtualFile root : classesRoots) {
            if (root.getName().equals(jarName)) {
                return root;
            }
        }

        return null;
    }
}
