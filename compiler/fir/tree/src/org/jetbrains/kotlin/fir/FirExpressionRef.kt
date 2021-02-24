/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.expressions.FirExpression

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class FirContractViolation

/**
 * This class is created to implicitly overcome FIR contract that some node may only be referenced once from its parents (not from any other node)
 * Thus, these kinds of secondary references may not be common FIR nodes since it would lead to visiting them twice during usual traversal
 * And this class is used to wrap such references
 */
class FirExpressionRef<T : FirExpression> @FirContractViolation constructor () {
    lateinit var value: T
    fun bind(value: T) {
        this.value = value
    }
}
