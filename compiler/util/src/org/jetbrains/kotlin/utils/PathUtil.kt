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

package org.jetbrains.kotlin.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import org.jetbrains.jps.model.java.impl.JavaSdkUtil

import java.io.File
import java.util.regex.Pattern

object PathUtil {
    const val JS_LIB_JAR_NAME = "kotlin-stdlib-js.jar"
    const val JS_LIB_10_JAR_NAME = "kotlin-jslib.jar"
    const val ALLOPEN_PLUGIN_JAR_NAME = "allopen-compiler-plugin.jar"
    const val NOARG_PLUGIN_JAR_NAME = "noarg-compiler-plugin.jar"
    const val SAM_WITH_RECEIVER_PLUGIN_JAR_NAME = "sam-with-receiver-compiler-plugin.jar"
    const val JS_LIB_SRC_JAR_NAME = "kotlin-stdlib-js-sources.jar"
    const val KOTLIN_JAVA_RUNTIME_JRE7_JAR = "kotlin-stdlib-jre7.jar"
    const val KOTLIN_JAVA_RUNTIME_JRE8_JAR = "kotlin-stdlib-jre8.jar"
    const val KOTLIN_JAVA_RUNTIME_JRE7_SRC_JAR = "kotlin-stdlib-jre7-sources.jar"
    const val KOTLIN_JAVA_RUNTIME_JRE8_SRC_JAR = "kotlin-stdlib-jre8-sources.jar"
    const val KOTLIN_JAVA_STDLIB_JAR = "kotlin-stdlib.jar"
    const val KOTLIN_JAVA_REFLECT_JAR = "kotlin-reflect.jar"
    const val KOTLIN_JAVA_SCRIPT_RUNTIME_JAR = "kotlin-script-runtime.jar"
    const val KOTLIN_TEST_JAR = "kotlin-test.jar"
    const val KOTLIN_TEST_JS_JAR = "kotlin-test-js.jar"
    const val KOTLIN_JAVA_STDLIB_SRC_JAR = "kotlin-stdlib-sources.jar"
    const val KOTLIN_JAVA_STDLIB_SRC_JAR_OLD = "kotlin-runtime-sources.jar"
    const val KOTLIN_REFLECT_SRC_JAR = "kotlin-reflect-sources.jar"
    const val KOTLIN_TEST_SRC_JAR = "kotlin-test-sources.jar"
    const val KOTLIN_COMPILER_JAR = "kotlin-compiler.jar"

    @JvmField
    val KOTLIN_RUNTIME_JAR_PATTERN: Pattern = Pattern.compile("kotlin-(stdlib|runtime)(-\\d[\\d.]+(-.+)?)?\\.jar")
    val KOTLIN_STDLIB_JS_JAR_PATTERN: Pattern = Pattern.compile("kotlin-stdlib-js.*\\.jar")
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
        get() = if (!pathUtilJar.isFile) {
            // Not running from a jar, i.e. it is it must be a unit test
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
