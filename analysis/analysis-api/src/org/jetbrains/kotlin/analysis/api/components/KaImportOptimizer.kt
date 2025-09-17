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

@KaIdeApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaImportOptimizer : KaSessionComponent {
    /**
     * A [FqName] which can be used to import the given symbol, or `null` if the symbol cannot be imported.
     */
    @KaIdeApi
    public val KaSymbol.importableFqName: FqName?
}

/**
 * @see KaImportOptimizer.importableFqName
 */
@KaContextParameterApi
@KaIdeApi
context(context: KaImportOptimizer)
public val KaSymbol.importableFqName: FqName?
    get() = with(context) { importableFqName }
