/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirContractDescriptionOwner : FirElement {
    override val source: FirSourceElement?
    val contractDescription: FirContractDescription

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitContractDescriptionOwner(this, data)

    fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirContractDescriptionOwner
}
