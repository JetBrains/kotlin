/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import java.nio.file.Files
import java.nio.file.Path

/**
 * The [BinaryClassFileHandle] of a [PathRootEntry]: a file together with the [JavaRoot] it was found in. Two handles are equal when they
 * have the same root id and the same path relative to the root.
 */
class PathClassFileHandle internal constructor(private val entry: PathRootEntry, private val root: JavaRoot) : BinaryClassFileHandle {
    override val nameWithoutExtension: String
        get() = entry.nameWithoutExtension

    override val extension: String
        get() = entry.extension.orEmpty()

    override val rootId: JvmClasspathRootId
        get() = root.id

    override val rootPath: Path?
        get() = root.libraryPath()

    override val pathForDiagnostics: String
        get() = entry.pathForDiagnostics

    override fun readBytes(): ByteArray = Files.readAllBytes(entry.path)

    override fun sibling(nameWithExtension: String): BinaryClassFileHandle? =
        entry.sibling(nameWithExtension)?.let { PathClassFileHandle(it, root) }

    override fun equals(other: Any?): Boolean =
        this === other ||
                other is PathClassFileHandle &&
                root.id == other.root.id &&
                entry.relativePath == other.entry.relativePath

    override fun hashCode(): Int = 31 * root.id.hashCode() + entry.relativePath.hashCode()

    override fun toString(): String = pathForDiagnostics
}
