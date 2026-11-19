/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle

/**
 * An element of a JVM classpath root [JavaRoot], normally file or a directory.
 */
interface JvmRootEntry {
    val name: String

    val nameWithoutExtension: String

    /** The extension of [name] without the dot, or `null` if the name has none. */
    val extension: String?

    val isDirectory: Boolean

    /** Whether the entry still exists. Within the lifetime of an index, entries of the index's roots are expected to stay valid. */
    val isValid: Boolean

    /** Finds the direct child with exactly the given [name] (case-sensitive). */
    fun findChild(name: String): JvmRootEntry?

    /** The direct children of this entry; empty for files. */
    val children: List<JvmRootEntry>

    /** Whether this entry or any entry below it, a file or a directory, has the given [extension]. */
    fun containsExtensionRecursively(extension: String): Boolean

    fun toHandle(root: JavaRoot): BinaryClassFileHandle
}

/** Creates the top [JvmRootEntry] of each [JavaRoot] of an index, selecting the file-system backend of the root. */
fun interface JvmRootEntryFactory {
    fun createRootEntry(root: JavaRoot): JvmRootEntry

    companion object {
        val VFS: JvmRootEntryFactory = JvmRootEntryFactory { VfsRootEntry(it.file) }
    }
}
