/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed interface FirContractDescriptionOwner : FirElement {
    override val source: KtSourceElement?
    val contractDescription: FirContractDescription


    fun replaceContractDescription(newContractDescription: FirContractDescription)
}

inline fun <D> FirContractDescriptionOwner.transformContractDescription(transformer: FirTransformer<D>, data: D): FirContractDescriptionOwner 
     = apply { replaceContractDescription(contractDescription.transform(transformer, data)) }
