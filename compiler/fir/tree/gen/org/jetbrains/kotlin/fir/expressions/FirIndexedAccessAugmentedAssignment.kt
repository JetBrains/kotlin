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
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Represents an augmented assignment with an indexed access as the receiver (e.g., `arr[i] += 1`)
 * **before** it gets resolved.
 *
 * After resolution, the call will be desugared into regular function calls,
 * either of the form `arr.set(i, arr.get(i).plus(1))` or `arr.get(i).plusAssign(1)`.
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.indexedAccessAugmentedAssignment]
 */
abstract class FirIndexedAccessAugmentedAssignment : FirPureAbstractElement(), FirStatement {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract val lhsGetCall: FirFunctionCall
    abstract val rhs: FirExpression
    abstract val operation: FirOperation
    abstract val calleeReference: FirReference
    abstract val arrayAccessSource: KtSourceElement?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitIndexedAccessAugmentedAssignment(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformIndexedAccessAugmentedAssignment(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirIndexedAccessAugmentedAssignment
}
