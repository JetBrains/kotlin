/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import java.nio.file.Path

/**
 * Shared local VFS used by the tests below to convert `@TempDir` paths into [VirtualFile]s
 * that `JavaClassFinderOverAstImpl` and `extractFileInfoLightweight` now consume. The instance
 * is stateless (no VFS refresh) so reusing it across tests is safe.
 */
internal val testLocalFs = KotlinLocalFileSystem()

internal fun Path.toVirtualFile(): VirtualFile =
    testLocalFs.findFileByNioFile(this)
        ?: error("Could not obtain VirtualFile for path: $this (does it exist?)")

open class JavaParsingTestBase {

    protected fun parseSource(source: String): Pair<JavaSyntaxNode, JavaResolutionContext> {
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        val context = JavaResolutionContext.create(root)
        return root to context
    }

    protected fun parseFirstClass(source: String): JavaClassOverAst {
        val (root, context) = parseSource(source)
        val classNode = root.children.first { it.type.toString() == "CLASS" }
        return JavaClassOverAst(classNode, context)
    }
}
