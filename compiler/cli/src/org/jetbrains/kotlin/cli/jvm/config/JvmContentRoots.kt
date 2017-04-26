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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.ContentRoot
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.KotlinSourceRoot
import java.io.File

data class JvmClasspathRoot(override val file: File): JvmContentRoot

data class JavaSourceRoot(override val file: File, val packagePrefix: String?): JvmContentRoot

interface JvmContentRoot : ContentRoot {
    val file: File
}

fun CompilerConfiguration.addJvmClasspathRoot(file: File) {
    add(JVMConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(file))
}

fun CompilerConfiguration.addJvmClasspathRoots(files: List<File>): Unit = files.forEach { addJvmClasspathRoot(it) }

val CompilerConfiguration.jvmClasspathRoots: List<File>
    get() {
        return get(JVMConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<JvmClasspathRoot>()?.map { it.file } ?: emptyList()
    }

@JvmOverloads fun CompilerConfiguration.addJavaSourceRoot(file: File, packagePrefix: String? = null) {
    add(JVMConfigurationKeys.CONTENT_ROOTS, JavaSourceRoot(file, packagePrefix))
}

@JvmOverloads fun CompilerConfiguration.addJavaSourceRoots(files: List<File>, packagePrefix: String? = null): Unit =
        files.forEach { addJavaSourceRoot(it, packagePrefix) }

val CompilerConfiguration.javaSourceRoots: Set<String>
    get() = get(JVMConfigurationKeys.CONTENT_ROOTS)
            ?.mapNotNullTo(hashSetOf<String>()) {
                (it as? KotlinSourceRoot)?.path ?: (it as? JavaSourceRoot)?.file?.path
            }
            .orEmpty()