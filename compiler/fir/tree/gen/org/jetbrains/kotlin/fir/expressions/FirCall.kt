/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.call]
 */
sealed interface FirCall : FirStatement {
    override val source: KtSourceElement?
    override val annotations: List<FirAnnotation>
    val argumentList: FirArgumentList

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformCall(this, data) as E

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    fun replaceArgumentList(newArgumentList: FirArgumentList)

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCall
}
