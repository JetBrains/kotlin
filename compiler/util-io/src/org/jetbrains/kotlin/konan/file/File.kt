/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Deprecated(
    "Preserved for binary compatibility with existing versions of the kotlinx-benchmarks Gradle plugin. See KT-82882." +
            "\nPlease use java.nio.file.Path instead.",
    level = DeprecationLevel.ERROR
)
@Suppress("DEPRECATION_ERROR")
data class File(internal val javaPath: Path) {
    constructor(parent: File, child: String) : this(parent.javaPath.resolve(child))
    constructor(parent: File, child: File) : this(parent.javaPath.resolve(child.javaPath))
    constructor(path: String) : this(Paths.get(path))

    val path: String
        get() = javaPath.toString()
    val absolutePath: String
        get() = javaPath.toAbsolutePath().toString()
    val absoluteFile: File
        get() = File(absolutePath)
    val canonicalPath: String by lazy {
        javaPath.toFile().canonicalPath
    }
    val canonicalFile: File
        get() = File(canonicalPath)

    val name: String
        get() = javaPath.fileName.toString().removeSuffixIfPresent(java.io.File.separator) // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8153248
    val extension: String
        get() = name.substringAfterLast('.', "")
    val nameSegments: List<String>
        get() = javaPath.map { it.fileName.toString() }

    val exists: Boolean
        get() = Files.exists(javaPath)
    val isDirectory: Boolean
        get() = Files.isDirectory(javaPath)
    val isFile: Boolean
        get() = Files.isRegularFile(javaPath)
    val isAbsolute: Boolean
        get() = javaPath.isAbsolute
    val listFiles: List<File>
        get() = Files.newDirectoryStream(javaPath).use { stream -> stream.map(::File) }

    fun child(name: String): File = File(this, name)

    override fun toString(): String = path

    override fun equals(other: Any?): Boolean {
        val otherFile = other as? File ?: return false
        return otherFile.javaPath.toAbsolutePath() == javaPath.toAbsolutePath()
    }

    override fun hashCode(): Int = javaPath.toAbsolutePath().hashCode()
}
