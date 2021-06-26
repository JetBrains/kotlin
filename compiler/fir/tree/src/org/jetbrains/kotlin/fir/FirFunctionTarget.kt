/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

class FirFunctionTarget(
    labelName: String?,
    val isLambda: Boolean
) : FirAbstractTarget<FirFunction>(labelName) {
    private lateinit var targetSymbol: FirFunctionSymbol<*>

    override var _labeledElement: FirFunction
        get() = targetSymbol.fir
        set(value) {
            targetSymbol = value.symbol
        }
}
