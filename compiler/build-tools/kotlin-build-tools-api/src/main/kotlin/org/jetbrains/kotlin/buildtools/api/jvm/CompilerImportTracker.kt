/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * A tracker that will be informed of the import directives the compiler resolves.
 *
 * An import that is never used, or whose declaration leaves no reference in the compiled output, cannot be
 * rediscovered afterwards, so it can only be observed at compile time.
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface CompilerImportTracker {
    /**
     * A callback that will be invoked when the compiler resolves an import directive.
     *
     * @param filePath the source file containing the import directive
     * @param importedFqName the fully qualified name being imported, with all parts separated by `.`
     *   (for example, `com.example.Outer.Inner`)
     */
    public fun report(filePath: String, importedFqName: String)
}
