/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElement

class CompositeTransformResult<out T : Any>(val a: Any) {

    companion object {
        fun <T : Any> empty() = CompositeTransformResult<T>(emptyList<T>())
        fun <T : Any> single(t: T) = CompositeTransformResult<T>(t)
        fun <T : Any> many(l: List<T>) = CompositeTransformResult<T>(l)
    }


    val isSingle get() = a !is List<*>
    val isEmpty get() = a is List<*> && a.isEmpty()

    val single: T
        get() {
            assert(isSingle)
            return a as T
        }

    val list: List<T>
        get() {
            return a as List<T>
        }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : FirElement> T.compose() = CompositeTransformResult.single(this)
