/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirDeclaration : FirPureAbstractElement(), FirAnnotationContainer {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract val symbol: FirBasedSymbol<out FirDeclaration>
    abstract val moduleData: FirModuleData
    abstract val resolvePhase: FirResolvePhase
    abstract val origin: FirDeclarationOrigin
    abstract val attributes: FirDeclarationAttributes


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceResolvePhase(newResolvePhase: FirResolvePhase)
}

inline fun <D> FirDeclaration.transformAnnotations(transformer: FirTransformer<D>, data: D): FirDeclaration  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }
