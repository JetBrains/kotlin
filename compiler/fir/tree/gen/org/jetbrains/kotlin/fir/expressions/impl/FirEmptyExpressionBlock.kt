/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirEmptyExpressionBlock : FirBlock() {
    override val source: KtSourceElement? get() = null
    override val annotations: List<FirAnnotation> get() = emptyList()
    override val statements: List<FirStatement> get() = emptyList()
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override val elementKind get() = FirElementKind.Block

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {}

    override fun replaceStatements(newStatements: List<FirStatement>) {}

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
