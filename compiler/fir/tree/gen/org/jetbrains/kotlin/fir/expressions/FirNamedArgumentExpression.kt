/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

/**
 * Represents a named argument `foo = bar` before and during body resolution phase.
 *
 * After body resolution, all [FirNamedArgumentExpression]s are removed from the FIR tree and the argument mapping must be
 * retrieved from [FirResolvedArgumentList.mapping].
 *
 * For a named argument with spread operator `foo = *bar`, [isSpread] will be set to `true` but no additional
 * [FirSpreadArgumentExpression] will be created as the [expression].
 *
 * **Special case vor varargs**: named arguments for `vararg` parameters are replaced with [FirSpreadArgumentExpression] with
 * [FirSpreadArgumentExpression.isNamed] set to `true`.
 *
 * See [FirVarargArgumentsExpression] for the general structure of arguments of `vararg` parameters after resolution.
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.namedArgumentExpression]
 */
abstract class FirNamedArgumentExpression : FirWrappedArgumentExpression() {
    abstract override val source: KtSourceElement?
    @UnresolvedExpressionTypeAccess
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract override val expression: FirExpression
    abstract override val isSpread: Boolean
    abstract val name: Name

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitNamedArgumentExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformNamedArgumentExpression(this, data) as E

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirNamedArgumentExpression
}
