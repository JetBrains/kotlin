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

        // Phase 2 parallel verification: build the light-tree representation from a fresh
        // builder and assert its structure mirrors the materialised `JavaSyntaxNode` tree.
        // This guarantees that every source parsed by the existing test suite is structurally
        // identical under both representations, so Phase 3 model-class migration can proceed
        // without surprises from divergent trees.
        val builder2 = parseJavaToSyntaxTreeBuilder(source, 0)
        val lightTree = buildJavaLightTree(builder2, source)
        assertTreesEquivalent(root, lightTree, lightTree.getRoot())

        val context = JavaResolutionContext.create(root)
        return root to context
    }

    protected fun parseFirstClass(source: String): JavaClassOverAst {
        val (root, context) = parseSource(source)
        val classNode = root.children.first { it.type.toString() == "CLASS" }
        return JavaClassOverAst(classNode, context)
    }

    /**
     * Verifies that the light-tree subtree rooted at [lightNode] is structurally equivalent to
     * the materialised [psiNode] subtree.
     *
     * Equivalence means:
     *  - [JavaSyntaxNode.type] equals [JavaLightTree.getType] for each visited node.
     *  - [JavaSyntaxNode.startOffset] / [JavaSyntaxNode.endOffset] match the light-tree offsets.
     *  - [JavaSyntaxNode.text] matches the light-tree text.
     *  - `psiNode.children.size == lightTree.getChildren(lightNode).size`.
     *  - Pairwise recursion into children.
     *
     * On mismatch, throws [AssertionError] with enough context (paths, types, excerpts) to
     * locate the divergence.
     */
    private fun assertTreesEquivalent(
        psiNode: JavaSyntaxNode,
        lightTree: JavaLightTree,
        lightNode: JavaLightNode,
        path: String = "<root>",
    ) {
        val lightType = lightTree.getType(lightNode)
        check(psiNode.type == lightType) {
            "Tree type mismatch at $path: PSI=${psiNode.type}, Light=$lightType"
        }
        val lightStart = lightTree.getStartOffset(lightNode)
        val lightEnd = lightTree.getEndOffset(lightNode)
        check(psiNode.startOffset == lightStart && psiNode.endOffset == lightEnd) {
            "Offset mismatch at $path (${psiNode.type}): " +
                "PSI=[${psiNode.startOffset},${psiNode.endOffset}), Light=[$lightStart,$lightEnd)"
        }
        val lightText = lightTree.getText(lightNode).toString()
        check(psiNode.text == lightText) {
            "Text mismatch at $path (${psiNode.type}): " +
                "PSI=${psiNode.text.take(60).replace("\n", "\\n")}, " +
                "Light=${lightText.take(60).replace("\n", "\\n")}"
        }
        val psiChildren = psiNode.children
        val lightChildren = lightTree.getChildren(lightNode)
        check(psiChildren.size == lightChildren.size) {
            "Child-count mismatch at $path (${psiNode.type}): " +
                "PSI=${psiChildren.size} ${psiChildren.map { it.type }}, " +
                "Light=${lightChildren.size} ${lightChildren.map { lightTree.getType(it) }}"
        }
        for (i in psiChildren.indices) {
            assertTreesEquivalent(
                psiChildren[i], lightTree, lightChildren[i],
                path = "$path/${psiNode.type}[$i]",
            )
        }
    }
}
