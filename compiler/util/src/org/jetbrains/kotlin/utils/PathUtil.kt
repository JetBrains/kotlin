/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import org.jetbrains.jps.model.java.impl.JavaSdkUtil

import java.io.File
import java.util.regex.Pattern

object PathUtil {
    const val JS_LIB_NAME = "kotlin-stdlib-js"
    const val JS_LIB_JAR_NAME = "$JS_LIB_NAME.jar"

    const val JS_LIB_10_JAR_NAME = "kotlin-jslib.jar"
    const val ALLOPEN_PLUGIN_NAME = "allopen-compiler-plugin"
    const val ALLOPEN_PLUGIN_JAR_NAME = "$ALLOPEN_PLUGIN_NAME.jar"
    const val NOARG_PLUGIN_NAME = "noarg-compiler-plugin"
    const val NOARG_PLUGIN_JAR_NAME = "$NOARG_PLUGIN_NAME.jar"
    const val SAM_WITH_RECEIVER_PLUGIN_NAME = "sam-with-receiver-compiler-plugin"
    const val SAM_WITH_RECEIVER_PLUGIN_JAR_NAME = "$SAM_WITH_RECEIVER_PLUGIN_NAME.jar"
    const val SERIALIZATION_PLUGIN_NAME = "kotlinx-serialization-compiler-plugin"
    const val SERIALIZATION_PLUGIN_JAR_NAME = "$SERIALIZATION_PLUGIN_NAME.jar"
    const val JS_LIB_SRC_JAR_NAME = "kotlin-stdlib-js-sources.jar"

    const val KOTLIN_JAVA_RUNTIME_JRE7_NAME = "kotlin-stdlib-jre7"
    const val KOTLIN_JAVA_RUNTIME_JRE7_JAR = "$KOTLIN_JAVA_RUNTIME_JRE7_NAME.jar"
    const val KOTLIN_JAVA_RUNTIME_JRE7_SRC_JAR = "$KOTLIN_JAVA_RUNTIME_JRE7_NAME-sources.jar"

    const val KOTLIN_JAVA_RUNTIME_JDK7_NAME = "kotlin-stdlib-jdk7"
    const val KOTLIN_JAVA_RUNTIME_JDK7_JAR = "$KOTLIN_JAVA_RUNTIME_JDK7_NAME.jar"
    const val KOTLIN_JAVA_RUNTIME_JDK7_SRC_JAR = "$KOTLIN_JAVA_RUNTIME_JDK7_NAME-sources.jar"

    const val KOTLIN_JAVA_RUNTIME_JRE8_NAME = "kotlin-stdlib-jre8"
    const val KOTLIN_JAVA_RUNTIME_JRE8_JAR = "$KOTLIN_JAVA_RUNTIME_JRE8_NAME.jar"
    const val KOTLIN_JAVA_RUNTIME_JRE8_SRC_JAR = "$KOTLIN_JAVA_RUNTIME_JRE8_NAME-sources.jar"

    const val KOTLIN_JAVA_RUNTIME_JDK8_NAME = "kotlin-stdlib-jdk8"
    const val KOTLIN_JAVA_RUNTIME_JDK8_JAR = "$KOTLIN_JAVA_RUNTIME_JDK8_NAME.jar"
    const val KOTLIN_JAVA_RUNTIME_JDK8_SRC_JAR = "$KOTLIN_JAVA_RUNTIME_JDK8_NAME-sources.jar"

    const val KOTLIN_JAVA_STDLIB_NAME = "kotlin-stdlib"
    const val KOTLIN_JAVA_STDLIB_JAR = "$KOTLIN_JAVA_STDLIB_NAME.jar"
    const val KOTLIN_JAVA_STDLIB_SRC_JAR = "$KOTLIN_JAVA_STDLIB_NAME-sources.jar"

    const val KOTLIN_JAVA_REFLECT_NAME = "kotlin-reflect"
    const val KOTLIN_JAVA_REFLECT_JAR = "$KOTLIN_JAVA_REFLECT_NAME.jar"
    const val KOTLIN_REFLECT_SRC_JAR = "$KOTLIN_JAVA_REFLECT_NAME-sources.jar"

    const val KOTLIN_JAVA_SCRIPT_RUNTIME_NAME = "kotlin-script-runtime"
    const val KOTLIN_JAVA_SCRIPT_RUNTIME_JAR = "$KOTLIN_JAVA_SCRIPT_RUNTIME_NAME.jar"
    const val KOTLIN_SCRIPTING_COMMON_NAME = "kotlin-scripting-common"
    const val KOTLIN_SCRIPTING_COMMON_JAR = "$KOTLIN_SCRIPTING_COMMON_NAME.jar"
    const val KOTLIN_SCRIPTING_JVM_NAME = "kotlin-scripting-jvm"
    const val KOTLIN_SCRIPTING_JVM_JAR = "$KOTLIN_SCRIPTING_JVM_NAME.jar"
    const val KOTLIN_SCRIPTING_JS_NAME = "kotlin-scripting-js"
    const val KOTLIN_SCRIPTING_JS_JAR = "$KOTLIN_SCRIPTING_JS_NAME.jar"
    const val KOTLIN_DAEMON_NAME = "kotlin-daemon"
    const val KOTLIN_DAEMON_JAR = "$KOTLIN_SCRIPTING_JVM_NAME.jar"
    const val KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME = "kotlin-scripting-compiler"
    const val KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR = "$KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME.jar"
    const val KOTLINX_COROUTINES_CORE_NAME = "kotlinx-coroutines-core"
    const val KOTLINX_COROUTINES_CORE_JAR = "$KOTLINX_COROUTINES_CORE_NAME.jar"
    const val KOTLIN_SCRIPTING_COMPILER_IMPL_NAME = "kotlin-scripting-compiler-impl"
    const val KOTLIN_SCRIPTING_COMPILER_IMPL_JAR = "$KOTLIN_SCRIPTING_COMPILER_IMPL_NAME.jar"
    const val JS_ENGINES_NAME = "js.engines"
    const val JS_ENGINES_JAR = "$JS_ENGINES_NAME.jar"
    const val MAIN_KTS_NAME = "kotlin-main-kts"

    val KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS = arrayOf(
        KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR, KOTLIN_SCRIPTING_COMPILER_IMPL_JAR,
        KOTLINX_COROUTINES_CORE_JAR,
        KOTLIN_SCRIPTING_COMMON_JAR, KOTLIN_SCRIPTING_JVM_JAR,
        KOTLIN_SCRIPTING_JS_JAR, JS_ENGINES_JAR
    )

    const val KOTLIN_TEST_NAME = "kotlin-test"
    const val KOTLIN_TEST_JAR = "$KOTLIN_TEST_NAME.jar"
    const val KOTLIN_TEST_SRC_JAR = "$KOTLIN_TEST_NAME-sources.jar"

    const val KOTLIN_TEST_JS_NAME = "kotlin-test-js"
    const val KOTLIN_TEST_JS_JAR = "$KOTLIN_TEST_JS_NAME.jar"

    const val KOTLIN_JAVA_STDLIB_SRC_JAR_OLD = "kotlin-runtime-sources.jar"

    const val TROVE4J_NAME = "trove4j"
    const val TROVE4J_JAR = "$TROVE4J_NAME.jar"

    const val KOTLIN_COMPILER_NAME = "kotlin-compiler"
    const val KOTLIN_COMPILER_JAR = "$KOTLIN_COMPILER_NAME.jar"

    @JvmField
    val KOTLIN_RUNTIME_JAR_PATTERN: Pattern = Pattern.compile("kotlin-(stdlib|runtime)(-\\d[\\d.]+(-.+)?)?\\.jar")
    val KOTLIN_STDLIB_JS_JAR_PATTERN: Pattern = Pattern.compile("kotlin-stdlib-js.*\\.jar")
    val KOTLIN_STDLIB_COMMON_JAR_PATTERN: Pattern = Pattern.compile("kotlin-stdlib-common.*\\.jar")
    val KOTLIN_JS_LIBRARY_JAR_PATTERN: Pattern = Pattern.compile("kotlin-js-library.*\\.jar")

    const val HOME_FOLDER_NAME = "kotlinc"
    private val NO_PATH = File("<no_path>")

    @JvmStatic
    val kotlinPathsForIdeaPlugin: KotlinPaths
        get() = if (ApplicationManager.getApplication().isUnitTestMode)
            kotlinPathsForDistDirectory
        else
            KotlinPathsFromHomeDir(compilerPathForIdeaPlugin)

    @JvmStatic
    val kotlinPathsForCompiler: KotlinPaths
        get() = if (!pathUtilJar.isFile || !pathUtilJar.name.startsWith(KOTLIN_COMPILER_NAME)) {
            // PathUtil.class is located not in the kotlin-compiler*.jar, so it must be a test and we'll take KotlinPaths from "dist/"
            // (when running tests, PathUtil.class is in its containing module's artifact, i.e. util-{version}.jar)
            kotlinPathsForDistDirectory
        }
        else KotlinPathsFromHomeDir(compilerPathForCompilerJar)

    @JvmStatic
    val kotlinPathsForDistDirectory: KotlinPaths
        get() = KotlinPathsFromHomeDir(File("dist", HOME_FOLDER_NAME))

    private val compilerPathForCompilerJar: File
        get() {
            val jar = pathUtilJar
            if (!jar.exists()) return NO_PATH

            if (jar.name == KOTLIN_COMPILER_JAR) {
                val lib = jar.parentFile
                return lib.parentFile
            }

            return NO_PATH
        }

    private val compilerPathForIdeaPlugin: File
        get() {
            val jar = pathUtilJar
            if (!jar.exists()) return NO_PATH

            if (jar.name == "kotlin-plugin.jar") {
                val lib = jar.parentFile
                val pluginHome = lib.parentFile

                return File(pluginHome, HOME_FOLDER_NAME)
            }

            return NO_PATH
        }

    val pathUtilJar: File
        get() = getResourcePathForClass(PathUtil::class.java)

    @JvmStatic
    fun getResourcePathForClass(aClass: Class<*>): File {
        val path = "/" + aClass.name.replace('.', '/') + ".class"
        val resourceRoot = PathManager.getResourceRoot(aClass, path) ?: throw IllegalStateException("Resource not found: $path")
        return File(resourceRoot).absoluteFile
    }

    @JvmStatic
    fun getJdkClassesRootsFromCurrentJre(): List<File> =
            getJdkClassesRootsFromJre(System.getProperty("java.home"))

    @JvmStatic
    fun getJdkClassesRootsFromJre(javaHome: String): List<File> =
            JavaSdkUtil.getJdkClassesRoots(File(javaHome), true)

    @JvmStatic
    fun getJdkClassesRoots(jdkHome: File): List<File> =
            JavaSdkUtil.getJdkClassesRoots(jdkHome, false)
}
