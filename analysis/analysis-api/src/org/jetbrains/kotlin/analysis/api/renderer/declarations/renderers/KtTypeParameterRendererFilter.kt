/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters

public interface KtTypeParameterRendererFilter {
    context(KtAnalysisSession)
    public fun filter(typeParameter: KtTypeParameterSymbol, owner: KtSymbolWithTypeParameters): Boolean

    public object NO_FOR_CONSTURCTORS : KtTypeParameterRendererFilter {
        context(KtAnalysisSession)
        override fun filter(typeParameter: KtTypeParameterSymbol, owner: KtSymbolWithTypeParameters): Boolean {
            return owner !is KtConstructorSymbol
        }
    }

    public companion object {
        public operator fun invoke(predicate: context(KtAnalysisSession)(typeParameter: KtTypeParameterSymbol, owner: KtSymbolWithTypeParameters) -> Boolean): KtTypeParameterRendererFilter {
            return object : KtTypeParameterRendererFilter {
                context(KtAnalysisSession)
                override fun filter(typeParameter: KtTypeParameterSymbol, owner: KtSymbolWithTypeParameters): Boolean {
                    return predicate(this@KtAnalysisSession, typeParameter, owner)
                }
            }
        }
    }
}