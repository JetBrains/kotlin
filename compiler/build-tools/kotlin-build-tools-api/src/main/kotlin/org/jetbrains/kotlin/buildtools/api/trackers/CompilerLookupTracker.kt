/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.trackers

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * A tracker that will be informed whenever the compiler makes lookups for references.
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface CompilerLookupTracker {
    public enum class ScopeKind {
        PACKAGE,
        CLASSIFIER
    }

    /**
     * A callback that will be invoked when the compiler performs a lookup.
     *
     * For context, Kotlin code can refer to symbols from a variety of scopes.
     * Consider a function call:
     * it can be a regular member function, a global function,
     * an extension function, an `invoke` operator, a local function, and so on.
     * There's a similar variety of options for type names and public variables.
     *
     * So, when a specific symbol is added or removed,
     * Kotlin Compiler should check if it impacts the pre-existing use sites of symbols with the same name.
     *
     * Lookups solve this issue.
     * A singular lookup is a record that says “when compiling file [filePath],
     * compiler looked up the symbol [name] in scope [scopeFqName]”.
     * So, if a matching symbol gets created or removed,
     * Kotlin compiler could recompile [filePath] to verify that there is no ambiguous overloading,
     * and that either the correct overload is chosen, or an appropriate error is reported.
     *
     * @param filePath The source file, whose compilation requires to look up the symbol [scopeFqName]->[name]
     * @param scopeFqName Fully qualified name of the scope where compiler looked up the symbol [name]. For example, a package name.
     * @param scopeKind Type of scope. See [ScopeKind] for the available options
     * @param name The name of the symbol being looked up
     */
    public fun recordLookup(
        filePath: String,
        scopeFqName: String,
        scopeKind: ScopeKind,
        name: String,
    )

    /**
     * The tracker should drop any previously recorded lookups in this compilation when this is called.
     */
    public fun clear()
}