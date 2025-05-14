/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirStaticPhantomThisExpression(
    val classSymbol: FirClassLikeSymbol<*>,
    @property:UnresolvedExpressionTypeAccess override val coneTypeOrNull: ConeKotlinType? = null
) : FirExpression() {
    override val source: KtSourceElement? = null
    override val annotations: List<FirAnnotation> = emptyList()

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) { }
    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) { }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirExpression = this

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) { }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement = this
}