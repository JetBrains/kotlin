/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.CompactVirtualFileSet
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSet
import com.intellij.openapi.vfs.VirtualFileSetFactory

/**
 * A clone of a package-private [com.intellij.openapi.vfs.CompactVirtualFileSetFactory].
 */
@Suppress("UnstableApiUsage", "TestOnlyProblems")
internal object KotlinCoreVirtualFileSetFactory : VirtualFileSetFactory {
    override fun createCompactVirtualFileSet(): VirtualFileSet {
        return CompactVirtualFileSet()
    }

    override fun createCompactVirtualFileSet(files: MutableCollection<out VirtualFile>): VirtualFileSet {
        return CompactVirtualFileSet().apply { addAll(files) }
    }
}
