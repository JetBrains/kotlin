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

package org.jetbrains.kotlin.utils

import kotlin.collections.*

import java.io.File

class KotlinPathsFromHomeDir(
    override val homePath: File // kotlinc directory
) : KotlinPaths {

    override val libPath: File
        get() = File(homePath, "lib")

    override val stdlibPath: File
        get() = getLibraryFile(PathUtil.KOTLIN_JAVA_STDLIB_JAR)

    override val reflectPath: File
        get() = getLibraryFile(PathUtil.KOTLIN_JAVA_REFLECT_JAR)

    override val scriptRuntimePath: File
        get() = getLibraryFile(PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR)

    override val kotlinTestPath: File
        get() = getLibraryFile(PathUtil.KOTLIN_TEST_JAR)

    override val stdlibSourcesPath: File
        get() = getLibraryFile(PathUtil.KOTLIN_JAVA_STDLIB_SRC_JAR)

    override val jsStdLibJarPath: File
        get() = getLibraryFile(PathUtil.JS_LIB_JAR_NAME)

    override val jsStdLibSrcJarPath: File
        get() = getLibraryFile(PathUtil.JS_LIB_SRC_JAR_NAME)

    override val jsKotlinTestJarPath: File
        get() = getLibraryFile(PathUtil.KOTLIN_TEST_JS_JAR)

    override val allOpenPluginJarPath: File
        get() = getLibraryFile(PathUtil.ALLOPEN_PLUGIN_JAR_NAME)

    override val noArgPluginJarPath: File
        get() = getLibraryFile(PathUtil.NOARG_PLUGIN_JAR_NAME)

    override val samWithReceiverJarPath: File
        get() = getLibraryFile(PathUtil.SAM_WITH_RECEIVER_PLUGIN_JAR_NAME)

    override val trove4jJarPath: File
        get() = getLibraryFile(PathUtil.TROVE4J_NAME)

    override val compilerClasspath: List<File>
        get() = listOf(stdlibPath, reflectPath, scriptRuntimePath, trove4jJarPath)

    override val compilerPath: File
        get() = getLibraryFile(PathUtil.KOTLIN_COMPILER_JAR)

    private fun getLibraryFile(fileName: String): File {
        return File(libPath, fileName)
    }
}
