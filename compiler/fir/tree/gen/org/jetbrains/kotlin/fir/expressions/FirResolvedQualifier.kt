/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirResolvedQualifier : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract val packageFqName: FqName
    abstract val relativeClassFqName: FqName?
    abstract val classId: ClassId?
    abstract val symbol: FirClassLikeSymbol<*>?
    abstract val isNullableLHSForCallableReference: Boolean
    abstract val resolvedToCompanionObject: Boolean
    abstract val nonFatalDiagnostics: List<ConeDiagnostic>
    abstract val typeArguments: List<FirTypeProjection>


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean)

    abstract fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean)

    abstract fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)
}

inline fun <D> FirResolvedQualifier.transformTypeRef(transformer: FirTransformer<D>, data: D): FirResolvedQualifier  = 
    apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirResolvedQualifier.transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedQualifier  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirResolvedQualifier.transformTypeArguments(transformer: FirTransformer<D>, data: D): FirResolvedQualifier  = 
    apply { replaceTypeArguments(typeArguments.transform(transformer, data)) }
