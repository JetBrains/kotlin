/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import java.util.concurrent.ConcurrentHashMap

/**
 * What the classpath roots of an environment are: [JavaRoot]s and [JavaModule.Root]s are only identities, and the code creating a root
 * registers what it points to here.
 *
 * Code that needs the [VirtualFile] of a root (the PSI Java view, VFS entries, search scopes) asks [virtualFile]; the NIO entries are
 * created from [location]. A root that was not registered is an error, so that a root added without registration is found early.
 */
class JavaRootRegistry {
    private val files = ConcurrentHashMap<JvmClasspathRootId, VirtualFile>()
    private val locations = ConcurrentHashMap<JvmClasspathRootId, JvmRootLocation>()

    /**
     * Registers [file] as the root [id]. The first registration of an id wins: the roots are recomputed from the same content roots
     * (e.g. when the classpath is updated), which yields the same files.
     */
    fun register(file: VirtualFile, id: JvmClasspathRootId = file.asJvmClasspathRootId()): JvmClasspathRootId {
        files.putIfAbsent(id, file)
        return id
    }

    /** Registers [file] and returns its root; see [JavaRoot] for [id]. */
    fun javaRoot(
        file: VirtualFile,
        type: JavaRoot.RootType,
        prefixFqName: FqName? = null,
        id: JvmClasspathRootId = file.asJvmClasspathRootId(),
    ): JavaRoot = JavaRoot(register(file, id), type, prefixFqName)

    /** Registers [file] and returns its module root; see [JavaModule.Root] for [id]. */
    fun moduleRoot(
        file: VirtualFile,
        isBinary: Boolean,
        isBinarySignature: Boolean = false,
        id: JvmClasspathRootId = file.asJvmClasspathRootId(),
    ): JavaModule.Root = JavaModule.Root(register(file, id), isBinary, isBinarySignature)

    fun findVirtualFile(id: JvmClasspathRootId): VirtualFile? = files[id]

    fun virtualFile(id: JvmClasspathRootId): VirtualFile = files[id] ?: error("The classpath root '${id.id}' is not registered")

    fun virtualFile(root: JavaRoot): VirtualFile = virtualFile(root.id)

    fun virtualFile(root: JavaModule.Root): VirtualFile = virtualFile(root.id)

    /** Where the root [id] lies, derived from its [VirtualFile] on the first request. */
    fun location(id: JvmClasspathRootId): JvmRootLocation = locations.getOrPut(id) { JvmRootLocation.of(virtualFile(id)) }

    /** Entries over the [VirtualFile]s of the roots. */
    val vfsEntries: JvmRootEntryFactory = JvmRootEntryFactory { root -> VfsRootEntry(virtualFile(root)) }
}
