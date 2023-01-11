/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.contracts.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirResolvedContractDescriptionImpl(
    override val source: KtSourceElement?,
    override val effects: MutableList<FirEffectDeclaration>,
    override val unresolvedEffects: MutableList<FirStatement>,
) : FirResolvedContractDescription() {
    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> acceptChildren(visitor: VT, data: D) {
        effects.forEach { it.accept(visitor, data) }
        unresolvedEffects.forEach { it.accept(visitor, data) }
    }

    override fun <D, @Monomorphic TT: FirTransformer<D>> transformChildren(transformer: TT, data: D): FirResolvedContractDescriptionImpl {
        effects.transformInplace(transformer, data)
        unresolvedEffects.transformInplace(transformer, data)
        return this
    }
}
