/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithTypeParameters

public interface KaTypeParameterRendererFilter {
    public fun filter(
        analysisSession: KaSession,
        typeParameter: KaTypeParameterSymbol,
        owner: KaSymbolWithTypeParameters,
    ): Boolean

    public object NO_FOR_CONSTURCTORS : KaTypeParameterRendererFilter {
        override fun filter(
            analysisSession: KaSession,
            typeParameter: KaTypeParameterSymbol,
            owner: KaSymbolWithTypeParameters,
        ): Boolean {
            return owner !is KaConstructorSymbol
        }
    }

    public companion object {
        public operator fun invoke(
            predicate: KaSession.(typeParameter: KaTypeParameterSymbol, owner: KaSymbolWithTypeParameters) -> Boolean
        ): KaTypeParameterRendererFilter {
            return object : KaTypeParameterRendererFilter {
                override fun filter(
                    analysisSession: KaSession,
                    typeParameter: KaTypeParameterSymbol,
                    owner: KaSymbolWithTypeParameters,
                ): Boolean {
                    return predicate(analysisSession, typeParameter, owner)
                }
            }
        }
    }
}

public typealias KtTypeParameterRendererFilter = KaTypeParameterRendererFilter