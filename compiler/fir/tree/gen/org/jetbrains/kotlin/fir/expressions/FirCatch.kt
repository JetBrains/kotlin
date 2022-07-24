/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirCatch : FirPureAbstractElement(), FirElement {
    abstract override val source: KtSourceElement?
    abstract val parameter: FirValueParameter
    abstract val block: FirBlock


    abstract fun replaceParameter(newParameter: FirValueParameter)

    abstract fun replaceBlock(newBlock: FirBlock)
}

inline fun <D> FirCatch.transformParameter(transformer: FirTransformer<D>, data: D): FirCatch  = 
    apply { replaceParameter(parameter.transform(transformer, data)) }

inline fun <D> FirCatch.transformBlock(transformer: FirTransformer<D>, data: D): FirCatch  = 
    apply { replaceBlock(block.transform(transformer, data)) }
