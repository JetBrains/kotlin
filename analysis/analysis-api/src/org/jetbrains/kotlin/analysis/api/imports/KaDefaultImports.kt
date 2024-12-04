/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.imports

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * Represents information for [default imports](https://kotlinlang.org/docs/packages.html#default-imports) for a specific platform.
 */
@KaIdeApi
public interface KaDefaultImports {
    /**
     * A list of [ImportPath] with [KaDefaultImportPriority] that represents a list of imports which are implicitly present
     * by default in every file.
     *
     * Some of these imports are star imports, and from them, we exclude some specific paths. This information is present in [excludedFromDefaultImports].
     */
    public val defaultImports: List<KaDefaultImport>

    /**
     * A list of non-star import paths that are excluded from some star default imports provided by [defaultImports].
     */
    public val excludedFromDefaultImports: List<ImportPath>
}
