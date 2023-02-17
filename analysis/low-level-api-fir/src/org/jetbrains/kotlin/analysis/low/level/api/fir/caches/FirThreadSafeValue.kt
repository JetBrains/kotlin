/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches

import org.jetbrains.kotlin.fir.caches.FirLazyValue

internal class FirThreadSafeValue<V>(createValue: () -> V) : FirLazyValue<V>() {
    private val lazyValue by lazy(LazyThreadSafetyMode.SYNCHRONIZED, createValue)
    override fun getValue(): V = lazyValue
}