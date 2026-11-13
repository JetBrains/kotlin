/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import java.nio.file.Path

/** A `.class` file which exists only in memory: not a virtual file, and without a local root or siblings. */
class InMemoryClassFileHandleForTests(
    override val nameWithoutExtension: String,
    private val content: ByteArray = ByteArray(0),
) : BinaryClassFileHandle {
    override val extension: String get() = "class"
    override val rootId: JvmClasspathRootId get() = JvmClasspathRootId("memory:out")
    override val rootPath: Path? get() = null
    override val pathForDiagnostics: String get() = "memory:out/$nameWithoutExtension.$extension"
    override fun readBytes(): ByteArray = content
    override fun sibling(nameWithExtension: String): BinaryClassFileHandle? = null
}
