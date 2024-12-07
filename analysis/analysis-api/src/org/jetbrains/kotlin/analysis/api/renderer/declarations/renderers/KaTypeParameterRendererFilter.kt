/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol

@KaExperimentalApi
public interface KaTypeParameterRendererFilter {
    public fun filter(
        analysisSession: KaSession,
        typeParameter: KaTypeParameterSymbol,
        owner: KaDeclarationSymbol,
    ): Boolean

    @KaExperimentalApi
    public object NO_FOR_CONSTURCTORS : KaTypeParameterRendererFilter {
        override fun filter(
            analysisSession: KaSession,
            typeParameter: KaTypeParameterSymbol,
            owner: KaDeclarationSymbol,
        ): Boolean {
            return owner !is KaConstructorSymbol
        }
    }

    @KaExperimentalApi
    public companion object {
        public operator fun invoke(
            predicate: KaSession.(typeParameter: KaTypeParameterSymbol, owner: KaDeclarationSymbol) -> Boolean
        ): KaTypeParameterRendererFilter {
            return object : KaTypeParameterRendererFilter {
                override fun filter(
                    analysisSession: KaSession,
                    typeParameter: KaTypeParameterSymbol,
                    owner: KaDeclarationSymbol,
                ): Boolean {
                    return predicate(analysisSession, typeParameter, owner)
                }
            }
        }
    }
}
