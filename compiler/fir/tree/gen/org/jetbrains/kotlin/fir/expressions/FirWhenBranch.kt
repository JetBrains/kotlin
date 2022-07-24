/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirWhenBranch : FirPureAbstractElement(), FirElement {
    abstract override val source: KtSourceElement?
    abstract val condition: FirExpression
    abstract val result: FirBlock


    abstract fun replaceCondition(newCondition: FirExpression)

    abstract fun replaceResult(newResult: FirBlock)
}

inline fun <D> FirWhenBranch.transformCondition(transformer: FirTransformer<D>, data: D): FirWhenBranch  = 
    apply { replaceCondition(condition.transform(transformer, data)) }

inline fun <D> FirWhenBranch.transformResult(transformer: FirTransformer<D>, data: D): FirWhenBranch  = 
    apply { replaceResult(result.transform(transformer, data)) }
