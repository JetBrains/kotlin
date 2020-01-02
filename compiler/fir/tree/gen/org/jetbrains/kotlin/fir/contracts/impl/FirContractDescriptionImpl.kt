/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirContractDescriptionImpl(
    override val source: FirSourceElement?
) : FirContractDescription() {
    override val effects: MutableList<ConeEffectDeclaration> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirContractDescriptionImpl {
        return this
    }
}
