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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class PathUtil {

    public static final String JS_LIB_JAR_NAME = "kotlin-jslib.jar";
    public static final String JS_LIB_JS_NAME = "kotlinEcma3.js";
    public static final String JDK_ANNOTATIONS_JAR = "kotlin-jdk-annotations.jar";
    public static final String KOTLIN_JAVA_RUNTIME_JAR = "kotlin-runtime.jar";

    private static final File NO_PATH = new File("<no_path>");

    private PathUtil() {}

    @NotNull
    public static KotlinPaths getKotlinPathsForIdeaPlugin() {
        return new KotlinPathsFromHomeDir(getCompilerPathForIdeaPlugin());
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForJpsPlugin() {
        return new KotlinPathsFromHomeDir(getCompilerPathForJpsPlugin());
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForJpsPluginOrJpsTests() {
        if ("true".equalsIgnoreCase(System.getProperty("kotlin.jps.tests"))) {
            return getKotlinPathsForDistDirectory();
        }
        return getKotlinPathsForJpsPlugin();
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForCompiler() {
        if (!getPathUtilJar().isFile()) {
            // Not running from a jar, i.e. it is it must be a unit test
            return getKotlinPathsForDistDirectory();
        }
        return new KotlinPathsFromHomeDir(getCompilerPathForCompilerJar());
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForDistDirectory() {
        return new KotlinPathsFromHomeDir(new File("dist/kotlinc"));
    }

    @NotNull
    private static File getCompilerPathForCompilerJar() {
        File jar = getPathUtilJar();

        if (!jar.exists()) return NO_PATH;

        if (jar.getName().equals("kotlin-compiler.jar")) {
            File lib = jar.getParentFile();
            return lib.getParentFile();
        }

        return NO_PATH;
    }

    @NotNull
    private static File getCompilerPathForJpsPlugin() {
        File jar = getPathUtilJar();

        if (!jar.exists()) return NO_PATH;

        if (jar.getName().equals("kotlin-jps-plugin.jar")) {
            File pluginHome = jar.getParentFile().getParentFile().getParentFile();
            return new File(pluginHome, "kotlinc");
        }

        return NO_PATH;
    }

    @NotNull
    private static File getCompilerPathForIdeaPlugin() {
        File jar = getPathUtilJar();

        if (!jar.exists()) return NO_PATH;

        if (jar.getName().equals("kotlin-plugin.jar")) {
            File lib = jar.getParentFile();
            File pluginHome = lib.getParentFile();

            return new File(pluginHome, "kotlinc");
        }

        return NO_PATH;
    }

    private static File getPathUtilJar() {
        return getJarPathForClass(PathUtil.class);
    }

    @NotNull
    public static File getJarPathForClass(@NotNull Class aClass) {
        String resourceRoot = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
        return new File(resourceRoot).getAbsoluteFile();
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
    public static File findRtJar() {
        return findRtJar(System.getProperty("java.home"));
    }

    private static File findRtJar(String javaHome) {
        if (SystemInfo.isMac && !SystemInfo.isJavaVersionAtLeast("1.7")) {
            File classesJar = new File(new File(javaHome).getParentFile(), "Classes/classes.jar");
            if (classesJar.exists()) {
                return classesJar;
            }

            throw new IllegalArgumentException("No classes.jar found under " + classesJar.getParent());
        }
        else {
            File rtJar = new File(javaHome, "lib/rt.jar");
            if (rtJar.exists()) {
                return rtJar;
            }

            throw new IllegalArgumentException("No rt.jar found under " + rtJar.getParent());
        }
    }
}
