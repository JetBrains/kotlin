/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirDynamicTypeRef : FirPureAbstractElement(), FirTypeRefWithNullability {
    abstract override val source: FirSourceElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val isMarkedNullable: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitDynamicTypeRef(this, data)
}
