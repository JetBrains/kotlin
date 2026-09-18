/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi

/**
 * A tracker that will be informed whenever the compiler looks a reference up.
 *
 * A name can be resolved in a variety of scopes, and the compiled output keeps only the scope that won. Adding or
 * removing a declaration elsewhere may change that outcome, which can only be told from the scopes that were
 * consulted while compiling.
 *
 * @since 2.5.0
 */
@InternalBuildToolsApi
public interface CompilerLookupTracker {
    /**
     * The kind of scope a symbol was looked up in.
     */
    public enum class ScopeKind {
        PACKAGE,
        CLASSIFIER,
    }

    /**
     * A callback that will be invoked when the compiler looks a symbol up.
     *
     * A single call records that compiling [filePath] required looking up [name] in [scopeFqName]. A consumer that
     * keeps these records can recompile [filePath] once a matching declaration appears in or disappears from that
     * scope.
     *
     * @param filePath the source file whose compilation required the lookup
     * @param scopeFqName fully qualified name of the scope the symbol was looked up in, with all parts separated by
     *   `.` (for example, `com.example` for a package, or `com.example.Outer.Inner` for a classifier)
     * @param scopeKind the kind of that scope
     * @param name the name of the symbol being looked up
     */
    public fun recordLookup(
        filePath: String,
        scopeFqName: String,
        scopeKind: ScopeKind,
        name: String,
    )

    /**
     * A callback that will be invoked when the lookups recorded so far in this compilation no longer apply and the
     * tracker is expected to discard them.
     */
    public fun clear()
}
