/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.impl.JavaSdkUtil;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class PathUtil {

    public static final String JPS_KOTLIN_HOME_PROPERTY = "jps.kotlin.home";

    public static final String JS_LIB_JAR_NAME = "kotlin-stdlib-js.jar";
    public static final String JS_LIB_10_JAR_NAME = "kotlin-jslib.jar";
    public static final String ALLOPEN_PLUGIN_JAR_NAME = "allopen-compiler-plugin.jar";
    public static final String NOARG_PLUGIN_JAR_NAME = "noarg-compiler-plugin.jar";
    public static final String SAM_WITH_RECEIVER_PLUGIN_JAR_NAME = "sam-with-receiver-compiler-plugin.jar";
    public static final String JS_LIB_SRC_JAR_NAME = "kotlin-stdlib-js-sources.jar";
    public static final String KOTLIN_JAVA_RUNTIME_JAR = "kotlin-runtime.jar";
    public static final String KOTLIN_JAVA_RUNTIME_JRE7_JAR = "kotlin-stdlib-jre7.jar";
    public static final String KOTLIN_JAVA_RUNTIME_JRE8_JAR = "kotlin-stdlib-jre8.jar";
    public static final String KOTLIN_JAVA_RUNTIME_JRE7_SRC_JAR = "kotlin-stdlib-jre7-sources.jar";
    public static final String KOTLIN_JAVA_RUNTIME_JRE8_SRC_JAR = "kotlin-stdlib-jre8-sources.jar";
    public static final String KOTLIN_JAVA_STDLIB_JAR = "kotlin-stdlib.jar";
    public static final String KOTLIN_JAVA_REFLECT_JAR = "kotlin-reflect.jar";
    public static final String KOTLIN_JAVA_SCRIPT_RUNTIME_JAR = "kotlin-script-runtime.jar";
    public static final String KOTLIN_TEST_JAR = "kotlin-test.jar";
    public static final String KOTLIN_TEST_JS_JAR = "kotlin-test-js.jar";
    public static final String KOTLIN_JAVA_STDLIB_SRC_JAR = "kotlin-stdlib-sources.jar";
    public static final String KOTLIN_JAVA_STDLIB_SRC_JAR_OLD = "kotlin-runtime-sources.jar";
    public static final String KOTLIN_REFLECT_SRC_JAR = "kotlin-reflect-sources.jar";
    public static final String KOTLIN_TEST_SRC_JAR = "kotlin-test-sources.jar";
    public static final String KOTLIN_COMPILER_JAR = "kotlin-compiler.jar";

    public static final Pattern KOTLIN_RUNTIME_JAR_PATTERN = Pattern.compile("kotlin-(stdlib|runtime)(-\\d[\\d.]+(-.+)?)?\\.jar");
    public static final Pattern KOTLIN_STDLIB_JS_JAR_PATTERN = Pattern.compile("kotlin-stdlib-js.*\\.jar");
    public static final Pattern KOTLIN_JS_LIBRARY_JAR_PATTERN = Pattern.compile("kotlin-js-library.*\\.jar");

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

        if (jar.getName().equals(KOTLIN_COMPILER_JAR)) {
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
        String path = "/" + aClass.getName().replace('.', '/') + ".class";
        String resourceRoot = PathManager.getResourceRoot(aClass, path);
        if (resourceRoot == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        return new File(resourceRoot).getAbsoluteFile();
    }

    @NotNull
    public static List<File> getJdkClassesRootsFromCurrentJre() {
        return getJdkClassesRootsFromJre(System.getProperty("java.home"));
    }

    @NotNull
    public static List<File> getJdkClassesRootsFromJre(@NotNull String javaHome) {
        return JavaSdkUtil.getJdkClassesRoots(new File(javaHome), true);
    }

    @NotNull
    public static List<File> getJdkClassesRoots(@NotNull File jdkHome) {
        return JavaSdkUtil.getJdkClassesRoots(jdkHome, false);
    }
}
