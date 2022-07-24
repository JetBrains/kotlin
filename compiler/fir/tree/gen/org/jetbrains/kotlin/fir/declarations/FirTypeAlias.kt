/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTypeAlias : FirClassLikeDeclaration(), FirTypeParameterRefsOwner {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val status: FirDeclarationStatus
    abstract override val deprecation: DeprecationsPerUseSite?
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract val name: Name
    abstract override val symbol: FirTypeAliasSymbol
    abstract val expandedTypeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>


    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract fun replaceExpandedTypeRef(newExpandedTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)
}

inline fun <D> FirTypeAlias.transformStatus(transformer: FirTransformer<D>, data: D): FirTypeAlias 
     = apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirTypeAlias.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirTypeAlias 
     = apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirTypeAlias.transformExpandedTypeRef(transformer: FirTransformer<D>, data: D): FirTypeAlias 
     = apply { replaceExpandedTypeRef(expandedTypeRef.transform(transformer, data)) }

inline fun <D> FirTypeAlias.transformAnnotations(transformer: FirTransformer<D>, data: D): FirTypeAlias 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }
