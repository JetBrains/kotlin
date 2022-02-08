/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

public abstract class KtImportOptimizer : ValidityTokenOwner {
    public abstract fun analyseImports(file: KtFile): KtImportOptimizerResult
}

public interface KtImportOptimizerMixIn : KtAnalysisSessionMixIn {

    /**
     * Takes [file] and inspects its imports and their usages,
     * so they can be optimized based on the resulting [KtImportOptimizerResult].
     *
     * Does **not** change the file.
     */
    public fun analyseImports(file: KtFile): KtImportOptimizerResult {
        return analysisSession.importOptimizer.analyseImports(file)
    }
}

public class KtImportOptimizerResult(
    public val unusedImports: Set<KtImportDirective>,
)