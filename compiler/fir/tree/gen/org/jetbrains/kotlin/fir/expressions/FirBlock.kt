/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirBlock : FirPureAbstractElement(), FirExpression {
    abstract override val source: FirSourceElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract val statements: List<FirStatement>
    abstract override val typeRef: FirTypeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)
}
