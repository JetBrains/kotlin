/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTypeParameter : FirTypeParameterRef, FirDeclaration() {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract val name: Name
    abstract override val symbol: FirTypeParameterSymbol
    abstract val containingDeclarationSymbol: FirBasedSymbol<*>
    abstract val variance: Variance
    abstract val isReified: Boolean
    abstract val bounds: List<FirTypeRef>
    abstract override val annotations: List<FirAnnotation>


    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract fun replaceBounds(newBounds: List<FirTypeRef>)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)
}

inline fun <D> FirTypeParameter.transformBounds(transformer: FirTransformer<D>, data: D): FirTypeParameter  = 
    apply { replaceBounds(bounds.transform(transformer, data)) }

inline fun <D> FirTypeParameter.transformAnnotations(transformer: FirTransformer<D>, data: D): FirTypeParameter  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }
