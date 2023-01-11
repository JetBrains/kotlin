/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirResolvedContractDescription : FirContractDescription() {
    abstract override val source: KtSourceElement?
    abstract val effects: List<FirEffectDeclaration>
    abstract val unresolvedEffects: List<FirStatement>

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> accept(visitor: VT, data: D): R = visitor.visitResolvedContractDescription(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D, @Monomorphic TT: FirTransformer<D>> transform(transformer: TT, data: D): E = 
        transformer.transformResolvedContractDescription(this, data) as E
}
