/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirLabel : FirPureAbstractElement(), FirElement {
    abstract override val source: FirSourceElement?
    abstract val name: String

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitLabel(this, data)
}
