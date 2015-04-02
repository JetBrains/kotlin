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

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.ContentRoot
import java.io.File

public data class JvmClasspathRoot(public override val file: File): JvmContentRoot

public data class JavaSourceRoot(public override val file: File): JvmContentRoot

public trait JvmContentRoot : ContentRoot {
    public val file: File
}

public fun CompilerConfiguration.addJvmClasspathRoot(file: File) {
    add(CommonConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(file))
}

public fun CompilerConfiguration.addJvmClasspathRoots(files: List<File>): Unit = files.forEach { addJvmClasspathRoot(it) }

public val CompilerConfiguration.jvmClasspathRoots: List<File>
    get() {
        return get(CommonConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<JvmClasspathRoot>()?.map { it.file } ?: emptyList()
    }

public fun CompilerConfiguration.addJavaSourceRoot(file: File) {
    add(CommonConfigurationKeys.CONTENT_ROOTS, JavaSourceRoot(file))
}

public fun CompilerConfiguration.addJavaSourceRoots(files: List<File>): Unit = files.forEach { addJavaSourceRoot(it) }