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
import java.util.ArrayList;
import java.util.List;

public class PathUtil {

    public static String JS_LIB_JAR_NAME = "kotlin-jslib.jar";
    public static String JS_LIB_JS_NAME = "kotlinLib.js";

    private PathUtil() {}

    public static File getDefaultCompilerPath() {
        File plugin_jar_path = new File(getJarPathForClass(PathUtil.class));

        if (!plugin_jar_path.exists()) return null;

        if (plugin_jar_path.getName().equals("kotlin-plugin.jar")) {
            File lib = plugin_jar_path.getParentFile();
            File pluginHome = lib.getParentFile();

            File answer = new File(pluginHome, "kotlinc");

            return answer.exists() ? answer : null;
        }

        if (plugin_jar_path.getName().equals("kotlin-compiler.jar")) {
            File lib = plugin_jar_path.getParentFile();
            File answer = lib.getParentFile();
            return answer.exists() ? answer : null;
        }
        
        File current = new File("").getAbsoluteFile(); // CWD

        do {
            File atDevHome = new File(current, "dist/kotlinc");
            if (atDevHome.exists()) return atDevHome;
            current = current.getParentFile();
        } while (current != null);

        return null;
    }

    @Nullable
    public static File getDefaultRuntimePath() {
        return getFilePackedIntoLib("kotlin-runtime.jar");
    }

    @Nullable
    public static File getDefaultJsLibJsPath() {
        return getFilePackedIntoLib(JS_LIB_JS_NAME);
    }

    @Nullable
    public static File getDefaultJsLibJarPath() {
        return getFilePackedIntoLib(JS_LIB_JAR_NAME);
    }

    @Nullable
    private static File getFilePackedIntoLib(@NotNull String filePathFromLib) {
        File compilerPath = getDefaultCompilerPath();
        if (compilerPath == null) return null;

        File answer = new File(compilerPath, "lib/" + filePathFromLib);

        return answer.exists() ? answer : null;
    }

    @Nullable
    public static File getJdkAnnotationsPath() {
        return getFilePackedIntoLib("alt/kotlin-jdk-annotations.jar");
    }

    @NotNull
    public static String getJarPathForClass(@NotNull Class aClass) {
        String resourceRoot = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
        return new File(resourceRoot).getAbsoluteFile().getAbsolutePath();
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
}
