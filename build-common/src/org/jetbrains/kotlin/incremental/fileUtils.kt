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

package org.jetbrains.kotlin.incremental

import java.io.File
import java.io.IOException

fun File.isJavaFile() =
        extension.equals("java", ignoreCase = true)

fun File.isKotlinFile(sourceFilesExtensions: List<String>): Boolean =
    !isJavaFile() && sourceFilesExtensions.any { it.equals(extension, ignoreCase = true) }

fun File.isClassFile(): Boolean =
        extension.equals("class", ignoreCase = true)

/**
 * Deletes the contents of this directory (not the directory itself) if it exists, or creates the directory if it does not yet exist.
 *
 * If this is a regular file, this method will throw an exception.
 */
fun File.cleanDirectoryContents() {
    when {
        isDirectory -> listFiles()!!.forEach { it.forceDeleteRecursively() }
        isFile -> error("File.cleanDirectoryContents does not accept a regular file: $path")
        else -> forceMkdirs()
    }
}

/** Deletes this file or directory recursively (if it exists). */
fun File.forceDeleteRecursively() {
    if (!deleteRecursively()) {
        throw IOException("Could not delete '$path'")
    }
}

/**
 * Creates this directory (if it does not yet exist).
 *
 * If this is a regular file, this method will throw an exception.
 */
@Suppress("SpellCheckingInspection")
fun File.forceMkdirs() {
    when {
        this.isDirectory -> { /* Do nothing */ }
        this.isFile -> error("File.forceMkdirs does not accept a regular file: $path")
        else -> {
            // Note that if the directory already exists, mkdirs() will return `false`, but here we ensure that the directory does not exist
            // before calling mkdirs(), so it's safe to check the returned result of mkdirs() below.
            if (!mkdirs()) {
                throw IOException("Could not create directory '$path'")
            }
        }
    }
}
