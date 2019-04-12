/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import java.io.File

interface SourceFileToPathConverter {
    fun toPath(file: File): String
    fun toFile(path: String): File
}

fun SourceFileToPathConverter.toPaths(files: Collection<File>): List<String> =
    files.map { toPath(it) }

fun SourceFileToPathConverter.toFiles(paths: Collection<String>): List<File> =
    paths.map { toFile(it) }

object SourceFileToCanonicalPathConverter : SourceFileToPathConverter {
    override fun toPath(file: File): String = file.canonicalPath

    override fun toFile(path: String): File = File(path)
}
