/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic
import org.jetbrains.kotlin.name.Name

class FirAnnotationArgumentMappingImpl(
    override val source: KtSourceElement?,
    override val mapping: Map<Name, FirExpression>
) : FirAnnotationArgumentMapping() {
    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> acceptChildren(visitor: VT, data: D) {}

    override fun <D, @Monomorphic TT: FirTransformer<D>> transformChildren(transformer: TT, data: D): FirElement {
        return this
    }
}

object FirEmptyAnnotationArgumentMapping : FirAnnotationArgumentMapping() {
    override val source: KtSourceElement?
        get() = null
    override val mapping: Map<Name, FirExpression>
        get() = emptyMap()

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> acceptChildren(visitor: VT, data: D) {}

    override fun <D, @Monomorphic TT: FirTransformer<D>> transformChildren(transformer: TT, data: D): FirElement {
        return this
    }
}

fun FirResolvedArgumentList.toAnnotationArgumentMapping(): FirAnnotationArgumentMapping {
    return FirAnnotationArgumentMappingImpl(
        source = null,
        mapping = mapping.entries.associateBy(
            keySelector = { it.value.name },
            valueTransform = { it.key.unwrapArgument() }
        )
    )
}
