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
 * ### Up to and including body resolution phase
 *
 * Represents unresolved collection literal. During body resolution, it is replaced with resolved [FirFunctionCall]
 * to operator `of`.
 *
 * ### After body resolution phase / deserialized
 *
 * Represents array literals in annotation arguments or default parameter values.
 * Both original collection literals and explicit `arrayOf` (`intArrayOf`, `doubleArrayOf`, etc.) calls in annotations are
 * represented as [FirCollectionLiteral] nodes.
 *
 * The structure of its [argumentList] is the same as for [FirVarargArgumentsExpression] - both regular expressions
 * and [FirSpreadArgumentExpression]s are possible (consider `intArrayOf(0, *[1, 2, 3], 4)`).
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.collectionLiteral]
 */
abstract class FirCollectionLiteral : FirExpression(), FirCall {
    abstract override val source: KtSourceElement?
    @UnresolvedExpressionTypeAccess
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract override val argumentList: FirArgumentList

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitCollectionLiteral(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformCollectionLiteral(this, data) as E

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCollectionLiteral
}
