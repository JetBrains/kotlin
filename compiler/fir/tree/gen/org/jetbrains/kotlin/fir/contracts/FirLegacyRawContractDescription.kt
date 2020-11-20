/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirLegacyRawContractDescription : FirContractDescription() {
    abstract override val source: FirSourceElement?
    abstract val contractCall: FirFunctionCall

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitLegacyRawContractDescription(this, data)
}
