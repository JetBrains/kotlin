/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package org.jetbrains.jet.utils;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class PathUtil {

    public static final String JS_LIB_JAR_NAME = "kotlin-jslib.jar";
    public static final String JS_LIB_JS_NAME = "kotlinLib.js";
    public static final String JDK_ANNOTATIONS_JAR = "kotlin-jdk-annotations.jar";
    public static final String KOTLIN_COMPILER_JAR = "kotlin-compiler.jar";
    public static final String KOTLIN_RUNTIME_JAR = "kotlin-runtime.jar";

    private PathUtil() {}

    @Nullable
    public static File getRuntimePath(@Nullable File sdkHome) {
        return getFilePackedIntoLib(sdkHome, KOTLIN_RUNTIME_JAR);
    }

    @Nullable
    public static File getCompilerPath(@Nullable File sdkHome) {
        return getFilePackedIntoLib(sdkHome, KOTLIN_COMPILER_JAR);
    }

    @Nullable
    public static File getJsLibJsPath(@Nullable File sdkHome) {
        return getFilePackedIntoLib(sdkHome, JS_LIB_JS_NAME);
    }

    @Nullable
    public static File getJsLibJarPath(@Nullable File sdkHome) {
        return getFilePackedIntoLib(sdkHome, JS_LIB_JAR_NAME);
    }

    @Nullable
    public static File getJdkAnnotationsPath(@Nullable File sdkHome) {
        return getFilePackedIntoLib(sdkHome, JDK_ANNOTATIONS_JAR);
    }

    @Nullable
    private static File getFilePackedIntoLib(@Nullable File sdkHome, @NotNull String filePathFromLib) {
        if (sdkHome == null) return null;
        File answer = new File(sdkHome, "lib/" + filePathFromLib);
        return answer.exists() ? answer : null;
    }

    @Nullable
    public static File getSDKHomeByCompilerPath(@Nullable File compilerPath) {
        if (compilerPath == null) return null;
        File libDir = compilerPath.getParentFile();
        if (libDir == null) return null;
        File sdkHome = libDir.getParentFile();
        return sdkHome != null && sdkHome.exists() ? sdkHome : null;
    }

    @NotNull
    public static VirtualFile jarFileOrDirectoryToVirtualFile(@NotNull File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                return VirtualFileManager.getInstance()
                        .findFileByUrl("file://" + FileUtil.toSystemIndependentName(file.getAbsolutePath()));
            }
            else {
                return VirtualFileManager.getInstance().findFileByUrl("jar://" + FileUtil.toSystemIndependentName(file.getAbsolutePath()) + "!/");
            }
        }
        else {
            throw new IllegalStateException("Path " + file + " does not exist.");
        }
    }

    @NotNull
    public static String getJarPathForClass(@NotNull Class aClass) {
        String resourceRoot = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
        return new File(resourceRoot).getAbsoluteFile().getAbsolutePath();
    }

    public static File findRtJar() {
        String javaHome = System.getProperty("java.home");
        if ("jre".equals(new File(javaHome).getName())) {
            javaHome = new File(javaHome).getParent();
        }

        File rtJar = findRtJar(javaHome);

        if (rtJar == null || !rtJar.exists()) {
            throw new IllegalArgumentException("No JDK rt.jar found under " + javaHome);
        }

        return rtJar;
    }

    private static File findRtJar(String javaHome) {
        File rtJar = new File(javaHome, "jre/lib/rt.jar");
        if (rtJar.exists()) {
            return rtJar;
        }

        File classesJar = new File(new File(javaHome).getParentFile().getAbsolutePath(), "Classes/classes.jar");
        if (classesJar.exists()) {
            return classesJar;
        }
        return null;
    }
}
