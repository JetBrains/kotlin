/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtImportOptimizer : KtLifetimeOwner {
    public abstract fun analyseImports(file: KtFile): KtImportOptimizerResult
}

public interface KtImportOptimizerMixIn : KtAnalysisSessionMixIn {

    /**
     * Takes [file] and inspects its imports and their usages,
     * so they can be optimized based on the resulting [KtImportOptimizerResult].
     *
     * Does **not** change the file.
     */
    public fun analyseImports(file: KtFile): KtImportOptimizerResult = withValidityAssertion {
        return analysisSession.importOptimizer.analyseImports(file)
    }
}

public class KtImportOptimizerResult(
    public val usedDeclarations: Map<FqName, Set<Name>> = emptyMap(),
    public val unresolvedNames: Set<Name> = emptySet(),
)