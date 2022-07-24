/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirStatementStub
import org.jetbrains.kotlin.fir.visitors.FirElementKind
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object FirStubStatement : FirStatementStub() {
    override val source: KtSourceElement?
        get() = null

    override val annotations: List<FirAnnotation>
        get() = emptyList()

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        require(newAnnotations.isEmpty())
    }

    override val elementKind: FirElementKind
        get() = FirElementKind.StatementStub
}
