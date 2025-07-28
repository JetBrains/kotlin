/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

@KaIdeApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaImportOptimizer : KaSessionComponent {
    /**
     * Analyzes imports in the given [file] and returns a [KaImportOptimizerResult] which can later be used to optimize imports.
     * Does **not** change the file.
     */
    @KaIdeApi
    public fun analyzeImportsToOptimize(file: KtFile): KaImportOptimizerResult

    /**
     * A [FqName] which can be used to import the given symbol, or `null` if the symbol cannot be imported.
     */
    @KaIdeApi
    public val KaSymbol.importableFqName: FqName?
}

/**
 * The result of the import directive analysis.
 *
 * @see KaImportOptimizer.analyzeImportsToOptimize
 */
@KaIdeApi
public class KaImportOptimizerResult(
    public val usedDeclarations: Map<FqName, Set<Name>> = emptyMap(),
    public val unresolvedNames: Set<Name> = emptySet(),
)

/**
 * @see KaImportOptimizer.analyzeImportsToOptimize
 */
@KaContextParameterApi
@KaIdeApi
context(context: KaImportOptimizer)
public fun analyzeImportsToOptimize(file: KtFile): KaImportOptimizerResult {
    return with(context) { analyzeImportsToOptimize(file) }
}

/**
 * @see KaImportOptimizer.importableFqName
 */
@KaContextParameterApi
@KaIdeApi
context(context: KaImportOptimizer)
public val KaSymbol.importableFqName: FqName?
    get() = with(context) { importableFqName }
