/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import java.io.File

open class RelativeFileToPathConverter(baseDirFile: File?) : FileToPathConverter {
    private val baseDirPath = baseDirFile?.canonicalFile?.invariantSeparatorsPath

    override fun toPath(file: File): String {
        val path = file.canonicalFile.invariantSeparatorsPath
        return when {
            baseDirPath != null && path.startsWith(baseDirPath) ->
                PROJECT_DIR_PLACEHOLDER + path.substring(baseDirPath.length)
            else -> path
        }
    }

    override fun toFile(path: String): File =
        when {
            path.startsWith(PROJECT_DIR_PLACEHOLDER) -> {
                val basePath = baseDirPath ?: error("Could not get project root dir")
                File(basePath + path.substring(PROJECT_DIR_PLACEHOLDER.length))
            }
            else -> File(path)
        }

    private companion object {
        private const val PROJECT_DIR_PLACEHOLDER = "${'$'}PROJECT_DIR$"
    }
}