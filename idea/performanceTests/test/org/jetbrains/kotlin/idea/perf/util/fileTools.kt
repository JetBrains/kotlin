/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import com.intellij.util.io.exists
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun String.lastPathSegment() =
    Paths.get(this).last().toString()

fun exists(path: String, vararg paths: String) =
    Paths.get(path, *paths).toAbsolutePath().exists()

fun Path.copyRecursively(targetDirectory: Path) {
    val src = this
    Files.walk(src)
        .forEach { source -> Files.copy(source, targetDirectory.resolve(src.relativize(source)), StandardCopyOption.REPLACE_EXISTING) }
}

fun File.allFilesWithExtension(ext: String): Iterable<File> =
    walk().filter { it.isFile && it.extension.equals(ext, ignoreCase = true) }.toList()
