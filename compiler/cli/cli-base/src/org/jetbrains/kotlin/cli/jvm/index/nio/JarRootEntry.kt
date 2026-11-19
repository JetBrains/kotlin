/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.ZipEntryDescription
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import java.nio.file.Path

/**
 * A [JvmRootEntry] of a jar archive of a [JarFilePool]: the directory tree is built once from the central directory and is immutable
 * afterwards. An entry whose name ends with `/` is a directory, and so is every parent of an entry, whether the archive lists it or not.
 */
class JarRootEntry private constructor(
    private val pool: JarFilePool,
    /** The path of the archive. */
    val jar: Path,
    /** The slash-separated path of this entry in the archive; empty for the root entry. */
    val relativePath: String,
    override val name: String,
    private val parent: JarRootEntry?,
    /** The archive entry of a file; `null` for a directory. */
    private val description: ZipEntryDescription?,
) : JvmRootEntry {
    private val childrenByName: LinkedHashMap<String, JarRootEntry>? = if (description == null) LinkedHashMap() else null

    override val nameWithoutExtension: String
        get() = name.substringBeforeLast('.')

    override val extension: String?
        get() = if (name.contains('.')) name.substringAfterLast('.') else null

    override val isDirectory: Boolean
        get() = description == null

    override val isValid: Boolean
        get() = true

    override fun findChild(name: String): JarRootEntry? = childrenByName?.get(name)

    override val children: List<JvmRootEntry>
        get() = childrenByName?.values?.toList().orEmpty()

    override fun containsExtensionRecursively(extension: String): Boolean =
        this.extension == extension || childrenByName?.values?.any { it.containsExtensionRecursively(extension) } == true

    override fun toHandle(root: JavaRoot): BinaryClassFileHandle = JarClassFileHandle(this, root)

    /** Finds the entry at the slash-separated [relativePath] below this one. */
    fun findByRelativePath(relativePath: String): JarRootEntry? =
        relativePath.split('/').filter { it.isNotEmpty() }.fold(this as JarRootEntry?) { entry, name -> entry?.findChild(name) }

    internal fun readBytes(): ByteArray = pool.readBytes(jar, checkNotNull(description) { "Not a file: $this" })

    internal fun sibling(name: String): JarRootEntry? = parent?.findChild(name)

    internal val pathForDiagnostics: String
        get() = JvmClasspathRootId.of(jar).id + JAR_SEPARATOR + relativePath

    override fun equals(other: Any?): Boolean =
        this === other || other is JarRootEntry && jar == other.jar && relativePath == other.relativePath

    override fun hashCode(): Int = 31 * jar.hashCode() + relativePath.hashCode()

    override fun toString(): String = pathForDiagnostics

    companion object {
        private const val JAR_SEPARATOR = "!/"

        internal fun build(pool: JarFilePool, jar: Path, entries: List<ZipEntryDescription>): JarRootEntry {
            val root = JarRootEntry(pool, jar, relativePath = "", name = "", parent = null, description = null)
            val directories = HashMap<String, JarRootEntry>().apply { put("", root) }

            fun directory(relativePath: String): JarRootEntry = directories.getOrPut(relativePath) {
                val parentPath = relativePath.substringBeforeLast('/', missingDelimiterValue = "")
                val parent = directory(parentPath)
                val name = relativePath.substringAfterLast('/')
                val existing = parent.findChild(name)
                if (existing != null && existing.isDirectory) return@getOrPut existing
                JarRootEntry(pool, jar, relativePath, name, parent, description = null)
                    .also { parent.childrenByName!!.putIfAbsent(name, it) }
            }

            for (entry in entries) {
                val path = entry.relativePath.toString().trimStart('/')
                if (path.endsWith('/')) {
                    path.trimEnd('/').takeIf { it.isNotEmpty() }?.let(::directory)
                } else if (path.isNotEmpty()) {
                    val parent = directory(path.substringBeforeLast('/', missingDelimiterValue = ""))
                    val name = path.substringAfterLast('/')
                    parent.childrenByName!!.putIfAbsent(name, JarRootEntry(pool, jar, path, name, parent, entry))
                }
            }
            return root
        }
    }
}

/**
 * The [BinaryClassFileHandle] of a [JarRootEntry]: a file together with the [JavaRoot] it was found in. Two handles are equal when they
 * have the same root id and the same path in the archive.
 */
class JarClassFileHandle internal constructor(private val entry: JarRootEntry, private val root: JavaRoot) : BinaryClassFileHandle {
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
                entry.relativePath == other.entry.relativePath

    override fun hashCode(): Int = 31 * root.id.hashCode() + entry.relativePath.hashCode()

    override fun toString(): String = pathForDiagnostics
}
