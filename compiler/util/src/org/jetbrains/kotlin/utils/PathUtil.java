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

package org.jetbrains.kotlin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.impl.JavaSdkUtil;

import java.io.File;
import java.util.List;

public class PathUtil {

    public static final String JPS_KOTLIN_HOME_PROPERTY = "jps.kotlin.home";

    public static final String JS_LIB_JAR_NAME = "kotlin-jslib.jar";
    public static final String JS_LIB_SRC_JAR_NAME = "kotlin-jslib-sources.jar";
    public static final String JDK_ANNOTATIONS_JAR = "kotlin-jdk-annotations.jar";
    public static final String ANDROID_SDK_ANNOTATIONS_JAR = "kotlin-android-sdk-annotations.jar";
    public static final String KOTLIN_JAVA_RUNTIME_JAR = "kotlin-runtime.jar";
    public static final String KOTLIN_JAVA_REFLECT_JAR = "kotlin-reflect.jar";
    public static final String KOTLIN_JAVA_RUNTIME_SRC_JAR = "kotlin-runtime-sources.jar";
    public static final String HOME_FOLDER_NAME = "kotlinc";

    private static final File NO_PATH = new File("<no_path>");

    private PathUtil() {}

    @NotNull
    public static KotlinPaths getKotlinPathsForIdeaPlugin() {
        return ApplicationManager.getApplication().isUnitTestMode()
            ? getKotlinPathsForDistDirectory()
            : new KotlinPathsFromHomeDir(getCompilerPathForIdeaPlugin());
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForJpsPlugin() {
        // When JPS is run on TeamCity, it can not rely on Kotlin plugin layout,
        // so the path to Kotlin is specified in a system property
        String jpsKotlinHome = System.getProperty(JPS_KOTLIN_HOME_PROPERTY);
        if (jpsKotlinHome != null) {
            return new KotlinPathsFromHomeDir(new File(jpsKotlinHome));
        }
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
        return new KotlinPathsFromHomeDir(new File("dist", HOME_FOLDER_NAME));
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
            return new File(pluginHome, HOME_FOLDER_NAME);
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

            return new File(pluginHome, HOME_FOLDER_NAME);
        }

        return NO_PATH;
    }

    @NotNull
    public static File getPathUtilJar() {
        return getResourcePathForClass(PathUtil.class);
    }

    @NotNull
    public static File getResourcePathForClass(@NotNull Class aClass) {
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
    public static List<File> getJdkClassesRoots() {
        return JavaSdkUtil.getJdkClassesRoots(new File(System.getProperty("java.home")), true);
    }
}
