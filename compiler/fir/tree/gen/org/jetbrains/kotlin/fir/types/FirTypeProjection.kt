/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTypeProjection : FirPureAbstractElement(), FirElement {
    abstract override val source: FirSourceElement?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitTypeProjection(this, data)
}
