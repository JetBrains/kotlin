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
 * A [VirtualFileSetFactory] the compiler registers as an application service, since the core application
 * environment does not provide one.
 *
 * It constructs [CompactVirtualFileSet] directly through its public API. Note that it must NOT delegate to
 * `VfsUtilCore.createCompactVirtualFileSet`: that method resolves the registered `VirtualFileSetFactory`
 * service -- which is this very object -- and would recurse into a `StackOverflowError`.
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
