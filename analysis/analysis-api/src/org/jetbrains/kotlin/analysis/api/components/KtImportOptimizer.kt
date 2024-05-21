/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

public abstract class KaImportOptimizer : KaLifetimeOwner {
    public abstract fun analyseImports(file: KtFile): KaImportOptimizerResult

    public abstract fun getImportableName(symbol: KaSymbol): FqName?
}

public typealias KtImportOptimizer = KaImportOptimizer

public interface KaImportOptimizerMixIn : KaSessionMixIn {

    /**
     * Takes [file] and inspects its imports and their usages,
     * so they can be optimized based on the resulting [KaImportOptimizerResult].
     *
     * Does **not** change the file.
     */
    public fun analyseImports(file: KtFile): KaImportOptimizerResult = withValidityAssertion {
        return analysisSession.importOptimizer.analyseImports(file)
    }

    /**
     * @return a [FqName] which can be used to import [this] symbol or `null` if the symbol cannot be imported.
     */
    public fun KaSymbol.getImportableName(): FqName? = withValidityAssertion {
        return analysisSession.importOptimizer.getImportableName(this)
    }
}

public typealias KtImportOptimizerMixIn = KaImportOptimizerMixIn

public class KaImportOptimizerResult(
    public val usedDeclarations: Map<FqName, Set<Name>> = emptyMap(),
    public val unresolvedNames: Set<Name> = emptySet(),
)

public typealias KtImportOptimizerResult = KaImportOptimizerResult