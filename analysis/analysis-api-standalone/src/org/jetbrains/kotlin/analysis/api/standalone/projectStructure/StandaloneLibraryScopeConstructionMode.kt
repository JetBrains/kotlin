/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.projectStructure

import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.LibraryScopeConstructionMode

/**
 * The strategy used to build a library module's content scope from its binary roots in Standalone mode.
 *
 * The content scope of a library module determines the files belonging to the library. The construction mode allows picking the data
 * structure and algorithm that implements the content scope. This has a direct impact on the memory footprint and performance profile of
 * the scope check, and in general, Standalone's overall performance.
 *
 * [ParentTraversal] is the default. In most cases, it should be the most performant and most memory-friendly option. In *exceptional*
 * cases, particularly with non-standard virtual file system implementations, it may cause errors or performance issues. The other options
 * are provided as workarounds, with [Trie] being the recommended one.
 *
 * The mode can be set per module on the module builder, or as a default for all modules on the module provider builder.
 */
@StandaloneWorkaroundApi
public sealed class StandaloneLibraryScopeConstructionMode {
    /**
     * Determines containment by walking a file's parents until one of them is a library root.
     *
     * In most cases, this mode should perform the best. It should also work with in-memory file systems. If the mode causes problems with a
     * particular file system, it is recommended to switch to [Trie].
     *
     * This is the default mode.
     */
    @StandaloneWorkaroundApi
    public data object ParentTraversal : StandaloneLibraryScopeConstructionMode()

    /**
     * Determines containment by matching a file's path segments against a trie built from the library root paths.
     *
     * This mode performs reasonably well, but is (likely) not as efficient as [ParentTraversal], because the containment checks heavily
     * allocate substrings. It is a workaround for rare cases when [ParentTraversal] might not function correctly.
     *
     * Because the trie relies on on-disk paths, this mode falls back to [Enumeration] when a library root lacks one (for example, with an
     * in-memory file system).
     */
    @StandaloneWorkaroundApi
    public data object Trie : StandaloneLibraryScopeConstructionMode()

    /**
     * Determines containment by checking a file against a precomputed set of all files reachable from the library roots.
     *
     * This mode is inefficient, as it eagerly enumerates and retains every file under each library root. It should only be used as a last
     * resort.
     */
    @StandaloneWorkaroundApi
    public data object Enumeration : StandaloneLibraryScopeConstructionMode()

    /**
     * A dummy private subtype to force `else` branches in client code so that new modes can be added in the future without breaking
     * compatibility.
     */
    @StandaloneWorkaroundApi
    @Suppress("unused")
    private data object Unknown : StandaloneLibraryScopeConstructionMode()
}

/**
 * Maps this public [StandaloneLibraryScopeConstructionMode] to the internal [LibraryScopeConstructionMode] understood by the Standalone
 * implementation.
 */
@OptIn(StandaloneWorkaroundApi::class)
internal fun StandaloneLibraryScopeConstructionMode.toInternalLibraryScopeConstructionMode(): LibraryScopeConstructionMode =
    when (this) {
        StandaloneLibraryScopeConstructionMode.ParentTraversal -> LibraryScopeConstructionMode.ParentTraversal
        StandaloneLibraryScopeConstructionMode.Trie -> LibraryScopeConstructionMode.Trie
        StandaloneLibraryScopeConstructionMode.Enumeration -> LibraryScopeConstructionMode.Enumeration

        // The sealed hierarchy has a private subtype to keep `when` expressions non-exhaustive for API evolution; it is never passed here.
        else -> error("Unexpected library scope construction mode: $this")
    }
