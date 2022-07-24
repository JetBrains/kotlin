/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirElementKind
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle

class FirSingleExpressionBlock(
    var statement: FirStatement
) : FirBlock() {
    override val source: KtSourceElement?
        get() = statement.source?.fakeElement(KtFakeSourceElementKind.SingleExpressionBlock)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val statements: List<FirStatement> get() = listOf(statement)
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceStatements(newStatements: List<FirStatement>) {
        require(newStatements.size == 1)
        statement = newStatements[0]
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override val elementKind: FirElementKind
        get() = FirElementKind.Block
}

@Suppress("NOTHING_TO_INLINE")
inline fun buildSingleExpressionBlock(statement: FirStatement): FirBlock {
    return FirSingleExpressionBlock(statement)
}
