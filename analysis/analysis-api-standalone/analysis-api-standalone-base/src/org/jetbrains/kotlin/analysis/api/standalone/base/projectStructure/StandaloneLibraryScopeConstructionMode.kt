/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

/**
 * The strategy used to build a library module's content scope from its binary roots in Standalone mode.
 *
 * It determines the memory footprint and performance profile of a library module's content scope. This can have a significant impact on
 * Standalone's performance.
 *
 * The mode can be set per module on the module builder, or as a default for all modules on the module provider builder.
 * [ParentTraversal] is the default.
 */
sealed class StandaloneLibraryScopeConstructionMode {
    /**
     * Determines containment by walking a file's parents until one of them is a library root.
     *
     * In most cases, this mode should perform the best. It should also work with in-memory file systems. If the mode causes problems with a
     * particular file system, it is recommended to switch to [Trie].
     *
     * This is the default mode.
     */
    data object ParentTraversal : StandaloneLibraryScopeConstructionMode()

    /**
     * Determines containment by matching a file's path segments against a trie built from the library root paths.
     *
     * This mode performs reasonably well, but is (likely) not as efficient as [ParentTraversal], because the containment checks heavily
     * allocate substrings.
     *
     * Because the trie relies on on-disk paths, this mode falls back to [Enumeration] when a library root lacks one (for example, with an
     * in-memory file system).
     */
    data object Trie : StandaloneLibraryScopeConstructionMode()

    /**
     * Determines containment by checking a file against a precomputed set of all files reachable from the library roots.
     *
     * This mode is inefficient, as it eagerly enumerates and retains every file under each library root.
     */
    data object Enumeration : StandaloneLibraryScopeConstructionMode()

    /**
     * A dummy private subtype to force `else` branches in client code so that new modes can be added in the future without breaking
     * compatibility.
     */
    @Suppress("unused")
    private data object Unknown : StandaloneLibraryScopeConstructionMode()
}
