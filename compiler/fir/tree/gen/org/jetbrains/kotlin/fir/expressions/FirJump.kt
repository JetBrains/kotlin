/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirTarget
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirJump<E : FirTargetElement> : FirExpression {
    override val source: FirSourceElement?
    override val typeRef: FirTypeRef
    override val annotations: List<FirAnnotationCall>
    val target: FirTarget<E>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitJump(this, data)
}
