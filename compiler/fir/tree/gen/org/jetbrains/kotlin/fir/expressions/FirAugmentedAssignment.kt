/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Represents an augmented assignment statement (e.g. `x += y`) **before** it gets resolved.
 * After resolution, it will be either represented as an assignment (`x = x.plus(y)`) or a call (`x.plusAssign(y)`). 
 *
 * Augmented assignments with an indexed access as receiver are represented as [FirIndexedAccessAugmentedAssignment]. 
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.augmentedAssignment]
 */
abstract class FirAugmentedAssignment : FirPureAbstractElement(), FirStatement {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract val operation: FirOperation
    abstract val leftArgument: FirExpression
    abstract val rightArgument: FirExpression

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAugmentedAssignment(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformAugmentedAssignment(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAugmentedAssignment

    abstract fun <D> transformLeftArgument(transformer: FirTransformer<D>, data: D): FirAugmentedAssignment

    abstract fun <D> transformRightArgument(transformer: FirTransformer<D>, data: D): FirAugmentedAssignment
}
