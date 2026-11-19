/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.JarArchive
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.JarTreeFactory
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.ZipEntryDescription
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import java.nio.file.Path

/**
 * A [JvmRootEntry] of a jar archive: a node of the tree that [build] creates from the FastJar [JarArchive], without virtual files.
 *
 * The archive comes from the FastJar [org.jetbrains.kotlin.cli.jvm.compiler.jarfs.JarArchiveCache] and is shared with the virtual
 * files of the same jar. The tree has the same entries as the virtual file tree and is immutable once built. As for [PathRootEntry],
 * the entries are always [valid][isValid], even if the jar changes on disk.
 */
class JarRootEntry private constructor(
    val archive: JarArchive,
    private val nameSequence: CharSequence,
    private val parent: JarRootEntry?,
    /** The central directory entry of a file; `null` for a directory. */
    private val entry: ZipEntryDescription?,
) : JvmRootEntry {
    private var childArray: Array<JarRootEntry> = NO_CHILDREN

    val jar: Path
        get() = archive.path

    /** The slash-separated path of this entry in the archive; empty for the root entry. */
    val relativePath: String
        get() {
            val parent = parent ?: return ""
            return if (parent.parent == null) name else parent.relativePath + "/" + name
        }

    override val name: String
        get() = nameSequence.toString()

    override val nameWithoutExtension: String
        get() = name.withoutExtension()

    override val extension: String?
        get() = name.extensionOrNull()

    override val isDirectory: Boolean
        get() = entry == null

    override val isValid: Boolean
        get() = true

    override fun findChild(name: String): JarRootEntry? = childArray.firstOrNull { it.nameSequence.contentEquals(name) }

    override val children: List<JvmRootEntry>
        get() = childArray.asList()

    override fun containsExtensionRecursively(extension: String): Boolean =
        this.extension == extension || childArray.any { it.containsExtensionRecursively(extension) }

    override fun toHandle(root: JavaRoot): BinaryClassFileHandle = JarClassFileHandle(this, root)

    /** Finds the entry at the slash-separated [relativePath] below this one. */
    fun findByRelativePath(relativePath: String): JarRootEntry? =
        relativePath.split('/').filter { it.isNotEmpty() }.fold(this as JarRootEntry?) { entry, name -> entry?.findChild(name) }

    internal fun readBytes(): ByteArray = archive.contentsToByteArray(checkNotNull(entry) { "Not a file: $this" })

    internal fun sibling(name: String): JarRootEntry? = parent?.findChild(name)

    internal val pathForDiagnostics: String
        get() = JvmClasspathRootId.of(jar).id + JAR_SEPARATOR + relativePath

    override fun toString(): String = pathForDiagnostics

    companion object {
        private const val JAR_SEPARATOR = "!/"

        private val NO_CHILDREN = emptyArray<JarRootEntry>()

        /** Builds the tree of the entries of [archive] and returns its root entry. */
        fun build(archive: JarArchive): JarRootEntry = archive.buildTree(TreeFactory(archive))
    }

    private class TreeFactory(private val archive: JarArchive) : JarTreeFactory<JarRootEntry> {
        override fun createRoot(): JarRootEntry = JarRootEntry(archive, "", parent = null, entry = null)

        override fun createDirectory(name: CharSequence, parent: JarRootEntry): JarRootEntry =
            JarRootEntry(archive, name, parent, entry = null)

        override fun createFile(name: CharSequence, parent: JarRootEntry, entry: ZipEntryDescription): JarRootEntry =
            JarRootEntry(archive, name, parent, entry)

        override fun setChildren(directory: JarRootEntry, children: List<JarRootEntry>) {
            if (children.isNotEmpty()) directory.childArray = children.toTypedArray()
        }
    }
}

/**
 * The [BinaryClassFileHandle] of a [JarRootEntry]. Two handles are equal when they have the same root id and the same path in the archive,
 * even if their entries are of different trees of the archive, e.g. of two indexes.
 */
class JarClassFileHandle internal constructor(private val entry: JarRootEntry, private val root: JavaRoot) : BinaryClassFileHandle {
    private val pathInArchive: String = entry.relativePath

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

    override fun readBytes(): ByteArray = entry.readBytes()

    override fun sibling(nameWithExtension: String): BinaryClassFileHandle? =
        entry.sibling(nameWithExtension)?.takeIf { !it.isDirectory }?.let { JarClassFileHandle(it, root) }

    override fun equals(other: Any?): Boolean =
        this === other ||
                other is JarClassFileHandle &&
                root.id == other.root.id &&
                pathInArchive == other.pathInArchive &&
                entry.jar == other.entry.jar

    override fun hashCode(): Int = 31 * root.id.hashCode() + pathInArchive.hashCode()

    override fun toString(): String = pathForDiagnostics
}
