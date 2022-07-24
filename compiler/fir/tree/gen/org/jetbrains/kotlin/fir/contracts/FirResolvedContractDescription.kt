/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.contracts

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirResolvedContractDescription : FirContractDescription() {
    abstract override val source: KtSourceElement?
    abstract val effects: List<FirEffectDeclaration>
    abstract val unresolvedEffects: List<FirStatement>


    abstract fun replaceEffects(newEffects: List<FirEffectDeclaration>)

    abstract fun replaceUnresolvedEffects(newUnresolvedEffects: List<FirStatement>)
}

inline fun <D> FirResolvedContractDescription.transformEffects(transformer: FirTransformer<D>, data: D): FirResolvedContractDescription  = 
    apply { replaceEffects(effects.transform(transformer, data)) }

inline fun <D> FirResolvedContractDescription.transformUnresolvedEffects(transformer: FirTransformer<D>, data: D): FirResolvedContractDescription  = 
    apply { replaceUnresolvedEffects(unresolvedEffects.transform(transformer, data)) }
