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
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirClassLikeDeclaration : FirMemberDeclaration(), FirStatement {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val symbol: FirClassLikeSymbol<out FirClassLikeDeclaration>
    abstract val deprecation: DeprecationsPerUseSite?


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)
}

inline fun <D> FirClassLikeDeclaration.transformAnnotations(transformer: FirTransformer<D>, data: D): FirClassLikeDeclaration  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirClassLikeDeclaration.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirClassLikeDeclaration  = 
    apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirClassLikeDeclaration.transformStatus(transformer: FirTransformer<D>, data: D): FirClassLikeDeclaration  = 
    apply { replaceStatus(status.transform(transformer, data)) }
