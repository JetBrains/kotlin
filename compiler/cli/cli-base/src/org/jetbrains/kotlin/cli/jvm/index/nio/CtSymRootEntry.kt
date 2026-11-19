/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle

/**
 * A package directory of a `ct.sym` module root, with the rules of
 * [CtSymDirectoryContainer][org.jetbrains.kotlin.cli.jvm.modules.CtSymDirectoryContainer].
 *
 * The entry merges the directories [parts] of one module in several release folders of `ct.sym` into one package tree. Its files are
 * the entries of the parts seen through [CtSymFileEntry], whose siblings come from all parts: `ct.sym` may keep `Outer.sig` and
 * `Outer$Nested.sig` in different release folders, e.g. `8/java.base/java/util/Base64.sig` and `89A/java.base/java/util/Base64$Encoder.sig`.
 */
class CtSymRootEntry private constructor(
    private val parts: List<JvmRootEntry>,
    private val packages: Map<String, Boolean>,
    private val currentPackage: String,
    private val skipPackageCheck: Boolean,
) : JvmRootEntry {
    override val name: String
        get() = currentPackage.substringAfterLast(".")

    override val nameWithoutExtension: String
        get() = name

    override val extension: String?
        get() = null

    override val isDirectory: Boolean
        get() = true

    override val isValid: Boolean
        get() = parts.all { it.isValid }

    override val children: List<JvmRootEntry> by lazy(LazyThreadSafetyMode.PUBLICATION) { computeChildren() }

    private val childrenByName: Map<String, JvmRootEntry> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LinkedHashMap<String, JvmRootEntry>().apply { children.forEach { putIfAbsent(it.name, it) } }
    }

    private fun computeChildren(): List<JvmRootEntry> {
        val children = parts.flatMap { it.children }
        if (children.isEmpty()) return emptyList()

        val isExported = skipPackageCheck || packages.getOrDefault(currentPackage, false)
        val containers = mutableMapOf<String, MutableList<JvmRootEntry>>()
        return children.mapNotNull { child ->
            when {
                child.isDirectory -> {
                    val childPackage = if (currentPackage.isEmpty()) child.name else currentPackage + "." + child.name
                    var addingEntry: CtSymRootEntry? = null
                    if (skipPackageCheck || packages.contains(childPackage)) {
                        containers.getOrPut(childPackage) {
                            mutableListOf<JvmRootEntry>().also {
                                addingEntry = CtSymRootEntry(it, packages, childPackage, skipPackageCheck)
                            }
                        }.add(child)
                    }
                    addingEntry
                }
                isExported -> CtSymFileEntry(child, this)
                else -> null
            }
        }
    }

    override fun findChild(name: String): JvmRootEntry? = childrenByName[name]

    override fun containsExtensionRecursively(extension: String): Boolean =
        children.any { it.containsExtensionRecursively(extension) }

    override fun toHandle(root: JavaRoot): BinaryClassFileHandle =
        throw UnsupportedOperationException("A ct.sym package directory has no class-file handle: $currentPackage")

    override fun toString(): String = "ct.sym package '$currentPackage' of $parts"

    companion object {
        /** The root entry of a `ct.sym` module root made up of the module directories [parts]. */
        fun root(parts: List<JvmRootEntry>, packages: Map<String, Boolean>, skipPackageCheck: Boolean): CtSymRootEntry =
            CtSymRootEntry(parts, packages, currentPackage = "", skipPackageCheck)
    }
}

/** A file [file] of a part of the `ct.sym` package [packageEntry]; its siblings are the files of the package in all parts. */
private class CtSymFileEntry(private val file: JvmRootEntry, private val packageEntry: CtSymRootEntry) : JvmRootEntry by file {
    override fun toHandle(root: JavaRoot): BinaryClassFileHandle = CtSymClassFileHandle(file.toHandle(root), packageEntry, root)

    override fun toString(): String = file.toString()
}

private class CtSymClassFileHandle(
    private val handle: BinaryClassFileHandle,
    private val packageEntry: CtSymRootEntry,
    private val root: JavaRoot,
) : BinaryClassFileHandle by handle {
    override fun sibling(nameWithExtension: String): BinaryClassFileHandle? =
        packageEntry.findChild(nameWithExtension)?.takeIf { !it.isDirectory }?.toHandle(root)

    override fun equals(other: Any?): Boolean = this === other || other is CtSymClassFileHandle && handle == other.handle

    override fun hashCode(): Int = handle.hashCode()

    override fun toString(): String = handle.toString()
}
