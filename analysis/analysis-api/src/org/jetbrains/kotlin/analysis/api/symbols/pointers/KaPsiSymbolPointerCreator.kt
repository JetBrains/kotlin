/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.psi.KtElement
import kotlin.reflect.KClass

@KaImplementationDetail
public interface KaPsiSymbolPointerCreator {
    /**
     * Returns `KaPsiSymbolPointer` for the given [element].
     * The [originalSymbol], when provided, must be a symbol calculated for [element]
     */
    public fun symbolPointer(element: KtElement, originalSymbol: KaSymbol? = null): KaSymbolPointer<KaSymbol>

    /**
     * Returns `KaPsiSymbolPointer` for the given [element].
     * The [originalSymbol], when provided, must be a symbol calculated for [element]
     * The restored symbol must be an instance of [expectedType].
     */
    public fun <S : KaSymbol> symbolPointerOfType(
        element: KtElement,
        expectedType: KClass<S>,
        originalSymbol: S? = null
    ): KaSymbolPointer<S>

    @KaImplementationDetail
    public companion object {
        private fun getInstance(project: Project): KaPsiSymbolPointerCreator = project.service()

        /**
         * Returns `KaPsiSymbolPointer` for the given [element]
         */
        public fun symbolPointer(element: KtElement): KaSymbolPointer<KaSymbol> = getInstance(element.project).symbolPointer(element)

        /**
         * Returns `KaPsiSymbolPointer` for the given [element].
         * The [originalSymbol], when provided, must be a symbol calculated for [element]
         * The restored symbol must be an instance of [expectedType].
         */
        public fun <S : KaSymbol> symbolPointerOfType(
            element: KtElement,
            expectedType: KClass<S>,
            originalSymbol: S? = null
        ): KaSymbolPointer<S> =
            getInstance(element.project).symbolPointerOfType(element, expectedType, originalSymbol)


        /**
         * Returns `KaPsiSymbolPointer` for the given [element].
         * The [originalSymbol], when provided, must be a symbol calculated for [element]
         */
        public inline fun <reified S : KaSymbol> symbolPointerOfType(
            element: KtElement,
            originalSymbol: S? = null
        ): KaSymbolPointer<S> =
            symbolPointerOfType(element, S::class, originalSymbol)
    }
}