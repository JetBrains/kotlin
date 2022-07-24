/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirContextReceiverArgumentListOwner : FirElement {
    override val source: KtSourceElement?
    val contextReceiverArguments: List<FirExpression>


    fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)
}

inline fun <D> FirContextReceiverArgumentListOwner.transformContextReceiverArguments(transformer: FirTransformer<D>, data: D): FirContextReceiverArgumentListOwner  = 
    apply { replaceContextReceiverArguments(contextReceiverArguments.transform(transformer, data)) }
