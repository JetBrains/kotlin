/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirResolvedContractDescriptionImpl(
    override val source: FirSourceElement?,
    override val effects: MutableList<FirEffectDeclaration>,
    override val unresolvedEffects: MutableList<FirStatement>,
) : FirResolvedContractDescription() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        effects.forEach { it.accept(visitor, data) }
        unresolvedEffects.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirResolvedContractDescriptionImpl {
        effects.transformInplace(transformer, data)
        unresolvedEffects.transformInplace(transformer, data)
        return this
    }
}
