/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirLegacyRawContractDescription : FirContractDescription() {
    abstract override val source: KtSourceElement?
    abstract val contractCall: FirFunctionCall


    abstract fun replaceContractCall(newContractCall: FirFunctionCall)
}

inline fun <D> FirLegacyRawContractDescription.transformContractCall(transformer: FirTransformer<D>, data: D): FirLegacyRawContractDescription 
     = apply { replaceContractCall(contractCall.transform(transformer, data)) }
