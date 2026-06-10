/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import java.nio.file.Path

internal fun createLibrarySearchScope(
    binaryRoots: Collection<Path>,
    binaryVirtualFiles: Collection<VirtualFile>,
    mode: StandaloneLibraryScopeConstructionMode,
    environment: CoreApplicationEnvironment,
    project: Project,
): GlobalSearchScope =
    when (mode) {
        StandaloneLibraryScopeConstructionMode.ParentTraversal ->
            createParentTraversalLibrarySearchScope(binaryRoots, binaryVirtualFiles, environment, project)

        StandaloneLibraryScopeConstructionMode.Trie ->
            // The trie relies on on-disk paths, so fall back to enumeration when a root lacks one (e.g. in-memory).
            if (binaryVirtualFiles.any { it.toNioPathOrNull() == null }) {
                createEnumerationLibrarySearchScope(binaryRoots, binaryVirtualFiles, environment, project)
            } else {
                createTrieLibrarySearchScope(binaryRoots, binaryVirtualFiles, environment, project)
            }

        StandaloneLibraryScopeConstructionMode.Enumeration ->
            createEnumerationLibrarySearchScope(binaryRoots, binaryVirtualFiles, environment, project)

        // The sealed hierarchy has a private subtype to keep `when` expressions non-exhaustive for API evolution; it is never passed here.
        else -> error("Unexpected library scope construction mode: $mode")
    }

internal fun createParentTraversalLibrarySearchScope(
    binaryRoots: Collection<Path>,
    binaryVirtualFiles: Collection<VirtualFile>,
    environment: CoreApplicationEnvironment,
    project: Project,
): GlobalSearchScope {
    val virtualFiles = StandaloneProjectFactory.getVirtualFilesForLibraryRoots(binaryRoots, environment) + binaryVirtualFiles
    return ParentTraversalLibrarySearchScope(virtualFiles, project)
}

private class ParentTraversalLibrarySearchScope(
    roots: List<VirtualFile>,
    project: Project,
) : GlobalSearchScope(project) {
    private val rootFiles: Set<VirtualFile> = roots.toHashSet()

    override fun contains(file: VirtualFile): Boolean {
        var current: VirtualFile? = file
        while (current != null) {
            if (current in rootFiles) return true
            current = current.parent
        }
        return false
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    override fun isSearchInLibraries(): Boolean = true

    override fun toString(): String = "Parent-traversal library search scope over $rootFiles"
}

internal fun createTrieLibrarySearchScope(
    binaryRoots: Collection<Path>,
    binaryVirtualFiles: Collection<VirtualFile>,
    environment: CoreApplicationEnvironment,
    project: Project,
): GlobalSearchScope {
    val virtualFiles = StandaloneProjectFactory.getVirtualFilesForLibraryRoots(binaryRoots, environment) + binaryVirtualFiles
    return TrieLibrarySearchScope(virtualFiles, project)
}

private class TrieLibrarySearchScope(
    roots: List<VirtualFile>,
    project: Project,
) : GlobalSearchScope(project) {
    private val trie: SimpleTrie = SimpleTrie(roots.map { it.path })

    override fun contains(file: VirtualFile): Boolean = trie.contains(file.path)

    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    override fun isSearchInLibraries(): Boolean = true

    override fun toString(): String = "Trie-based library search scope"
}

private class SimpleTrie(paths: List<String>) {
    class TrieNode {
        var isTerminal: Boolean = false
    }

    val root = TrieNode()

    private val m = mutableMapOf<Pair<TrieNode, String>, TrieNode>().apply {
        paths.forEach { path ->
            var p = root
            for (d in path.trim('/').split('/')) {
                p = getOrPut(Pair(p, d)) { TrieNode() }
            }
            p.isTerminal = true
        }
    }

    fun contains(s: String): Boolean {
        var p = root
        for (d in s.trim('/').split('/')) {
            p = m[Pair(p, d)]?.also {
                if (it.isTerminal)
                    return true
            } ?: return false
        }
        return false
    }
}

internal fun createEnumerationLibrarySearchScope(
    binaryRoots: Collection<Path>,
    binaryVirtualFiles: Collection<VirtualFile>,
    environment: CoreApplicationEnvironment,
    project: Project,
): GlobalSearchScope {
    @OptIn(KaImplementationDetail::class)
    val virtualFileUrls = buildSet {
        for (root in StandaloneProjectFactory.getVirtualFilesForLibraryRoots(binaryRoots, environment) + binaryVirtualFiles) {
            LibraryUtils.getAllVirtualFilesFromRoot(root, includeRoot = true)
                .mapTo(this) { it.url }
        }
    }

    return EnumerationLibrarySearchScope(virtualFileUrls, project)
}

private class EnumerationLibrarySearchScope(
    private val virtualFileUrls: Set<String>,
    project: Project,
) : GlobalSearchScope(project) {
    override fun contains(file: VirtualFile): Boolean = file.url in virtualFileUrls

    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    override fun isSearchInLibraries(): Boolean = true

    override fun toString(): String = "Enumeration-based library search scope over $virtualFileUrls"
}
