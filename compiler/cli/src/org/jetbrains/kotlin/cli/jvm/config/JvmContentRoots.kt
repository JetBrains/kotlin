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

package org.jetbrains.kotlin.cli.jvm.config

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

interface JvmContentRoot : ContentRoot {
    val file: File
}

data class JvmClasspathRoot(override val file: File) : JvmContentRoot

data class JavaSourceRoot(override val file: File, val packagePrefix: String?) : JvmContentRoot

data class JvmModulePathRoot(override val file: File) : JvmContentRoot

fun CompilerConfiguration.addJvmClasspathRoot(file: File) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(file))
}

fun CompilerConfiguration.addJvmClasspathRoots(files: List<File>) {
    files.forEach(this::addJvmClasspathRoot)
}

fun CompilerConfiguration.addJvmSdkRoots(files: List<File>) {
    addAll(CLIConfigurationKeys.CONTENT_ROOTS, 0, files.map(::JvmClasspathRoot))
}

val CompilerConfiguration.jvmClasspathRoots: List<File>
    get() = getList(CLIConfigurationKeys.CONTENT_ROOTS).filterIsInstance<JvmClasspathRoot>().map(JvmContentRoot::file)

@JvmOverloads
fun CompilerConfiguration.addJavaSourceRoot(file: File, packagePrefix: String? = null) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, JavaSourceRoot(file, packagePrefix))
}

@JvmOverloads
fun CompilerConfiguration.addJavaSourceRoots(files: List<File>, packagePrefix: String? = null) {
    files.forEach { addJavaSourceRoot(it, packagePrefix) }
}

val CompilerConfiguration.javaSourceRoots: Set<String>
    get() = getList(CLIConfigurationKeys.CONTENT_ROOTS).mapNotNullTo(linkedSetOf()) { root ->
        when (root) {
            is KotlinSourceRoot -> root.path
            is JavaSourceRoot -> root.file.path
            else -> null
        }
    }
