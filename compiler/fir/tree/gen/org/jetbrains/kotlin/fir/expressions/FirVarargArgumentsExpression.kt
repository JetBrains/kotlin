/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * [FirVarargArgumentsExpression]s are created during body resolution phase for arguments of `vararg` parameters.
 *
 * If one or multiple elements are passed to a `vararg` parameter, the will be wrapped with a [FirVarargArgumentsExpression]
 * and [arguments] will contain the individual elements.
 *
 * If a named argument is passed to a `vararg` parameter, [arguments] will contain a single [FirSpreadArgumentExpression]
 * with [FirSpreadArgumentExpression.isNamed] set to `true`.
 *
 * [FirSpreadArgumentExpression]s are kept as is in [arguments]. 
 *
 * If no element is passed to a `vararg` parameter, no [FirVarargArgumentsExpression] is created regardless of whether the
 * parameter has a default value.
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.varargArgumentsExpression]
 */
abstract class FirVarargArgumentsExpression : FirExpression() {
    abstract override val source: KtSourceElement?
    @UnresolvedExpressionTypeAccess
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract val arguments: List<FirExpression>
    abstract val coneElementTypeOrNull: ConeKotlinType?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitVarargArgumentsExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformVarargArgumentsExpression(this, data) as E

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirVarargArgumentsExpression
}
