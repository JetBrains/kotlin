/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirResolvedQualifier : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract val packageFqName: FqName
    abstract val relativeClassFqName: FqName?
    abstract val classId: ClassId?
    abstract val symbol: FirClassLikeSymbol<*>?
    abstract val isNullableLHSForCallableReference: Boolean
    abstract val resolvedToCompanionObject: Boolean
    abstract val canBeValue: Boolean
    abstract val isFullyQualified: Boolean
    abstract val nonFatalDiagnostics: List<ConeDiagnostic>
    abstract val typeArguments: List<FirTypeProjection>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitResolvedQualifier(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformResolvedQualifier(this, data) as E

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean)

    abstract fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean)

    abstract fun replaceCanBeValue(newCanBeValue: Boolean)

    abstract fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedQualifier

    abstract fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirResolvedQualifier
}
