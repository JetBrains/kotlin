/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import java.io.File

open class IncrementalFileToPathConverter(val rootProjectDir: File?) : FileToPathConverter {
    //project root dir
    private val projectDirPath = rootProjectDir?.normalize()?.absolutePath

    override fun toPath(file: File): String {
        val path = file.normalize().absolutePath
        return when {
            projectDirPath == null || !path.startsWith(projectDirPath) -> path
            else -> PROJECT_DIR_PLACEHOLDER + path.substring(projectDirPath.length)
        }
    }

    override fun toFile(path: String): File =
        when {
            rootProjectDir != null && path.startsWith(PROJECT_DIR_PLACEHOLDER) -> rootProjectDir.resolve(path.substring(PATH_PREFIX.length))
            else -> File(path)
        }

    private companion object {
        private const val PROJECT_DIR_PLACEHOLDER = "${'$'}PROJECT_DIR$"
        private const val PATH_PREFIX = "$PROJECT_DIR_PLACEHOLDER/"

    }
}