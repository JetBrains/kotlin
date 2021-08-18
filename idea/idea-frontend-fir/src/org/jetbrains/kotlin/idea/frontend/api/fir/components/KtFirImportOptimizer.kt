/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.idea.frontend.api.components.KtImportOptimizer
import org.jetbrains.kotlin.idea.frontend.api.components.KtImportOptimizerResult
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirImportOptimizer : KtImportOptimizer() {
    override fun analyseImports(file: KtFile): KtImportOptimizerResult {
        return KtImportOptimizerResult(emptySet())
    }
}
