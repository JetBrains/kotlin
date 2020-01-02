/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirExpression : FirStatement {
    override val source: FirSourceElement?
    val typeRef: FirTypeRef
    override val annotations: List<FirAnnotationCall>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitExpression(this, data)

    fun replaceTypeRef(newTypeRef: FirTypeRef)
}
