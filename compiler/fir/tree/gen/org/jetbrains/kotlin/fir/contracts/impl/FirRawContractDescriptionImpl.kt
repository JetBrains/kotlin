/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.contracts.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirRawContractDescriptionImpl(
    override val source: KtSourceElement?,
    override val rawEffects: MutableList<FirExpression>,
) : FirRawContractDescription() {
    override val elementKind get() = FirElementKind.RawContractDescription

    override fun replaceRawEffects(newRawEffects: List<FirExpression>) {
        rawEffects.clear()
        rawEffects.addAll(newRawEffects)
    }
}
