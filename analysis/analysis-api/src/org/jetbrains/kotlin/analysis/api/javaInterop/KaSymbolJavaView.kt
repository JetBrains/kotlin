/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtElement

/**
 * A view on Kotlin declarations from Java perspective.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSymbolJavaView<out T : KaSymbol> {
    /**
     * Pointer to the symbol represented by this view.
     */
    public val symbolPointer: KaSymbolPointer<T>?

    /**
     * [KtElement] from which this view was constructed from.
     *
     * Note that [kotlinOrigin] might not point to the exact represented declaration.
     * That's because some elements might be synthetic and not have a physical PSI.
     * Use [symbolPointer] as a source of truth as both physical and non-physical elements have a proper symbol.
     */
    public val kotlinOrigin: KtElement?

    /**
     * [KaModule] from which view is provided.
     * The module is used for providing proper actualizations for `expect` declarations.
     */
    public val useSiteModule: KaModule?
}
