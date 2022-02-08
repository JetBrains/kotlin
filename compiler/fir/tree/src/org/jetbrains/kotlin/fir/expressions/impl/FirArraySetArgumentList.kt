/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAbstractArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression

class FirArraySetArgumentList internal constructor(
    private val rValue: FirExpression,
    private val indexes: List<FirExpression>
) : FirAbstractArgumentList() {
    override val arguments: List<FirExpression>
        get() = indexes + rValue

    override val source: KtSourceElement?
        get() = null
}
