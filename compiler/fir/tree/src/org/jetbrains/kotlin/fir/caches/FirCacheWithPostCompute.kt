/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

abstract class FirCache<in KEY : Any, out VALUE, in CONTEXT> {
    abstract fun getValue(key: KEY, context: CONTEXT): VALUE
    abstract fun getValueIfComputed(key: KEY): VALUE?
}

@Suppress("NOTHING_TO_INLINE")
inline fun <KEY : Any, VALUE> FirCache<KEY, VALUE, Nothing?>.getValue(key: KEY): VALUE =
    getValue(key, null)
