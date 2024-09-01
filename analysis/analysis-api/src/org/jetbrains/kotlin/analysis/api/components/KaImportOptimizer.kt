/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

@KaIdeApi
public interface KaImportOptimizer {
    /**
     * Analyzes imports in the given [file] and returns a [KaImportOptimizerResult] which can later be used to optimize imports.
     * Does **not** change the file.
     */
    @KaIdeApi
    public fun analyzeImportsToOptimize(file: KtFile): KaImportOptimizerResult

    @KaIdeApi
    @Deprecated("Use 'analyzeImportsToOptimize()' instead.", replaceWith = ReplaceWith("analyzeImportsToOptimize()"))
    public fun analyseImports(file: KtFile): KaImportOptimizerResult {
        return analyzeImportsToOptimize(file)
    }

    /**
     * A [FqName] which can be used to import the given symbol or `null` if the symbol cannot be imported.
     */
    @KaIdeApi
    public val KaSymbol.importableFqName: FqName?
}

/**
 * Result of the import directive analysis.
 */
@KaIdeApi
public class KaImportOptimizerResult(
    public val usedDeclarations: Map<FqName, Set<Name>> = emptyMap(),
    public val unresolvedNames: Set<Name> = emptySet(),
)

@KaIdeApi
@Deprecated("Use 'KaImportOptimizerResult' instead", ReplaceWith("KaImportOptimizerResult"))
public typealias KtImportOptimizerResult = KaImportOptimizerResult