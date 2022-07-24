/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.contracts.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirLegacyRawContractDescriptionImpl(
    override val source: KtSourceElement?,
    override var contractCall: FirFunctionCall,
) : FirLegacyRawContractDescription() {
    override val elementKind get() = FirElementKind.LegacyRawContractDescription

    override fun replaceContractCall(newContractCall: FirFunctionCall) {
        contractCall = newContractCall
    }
}
