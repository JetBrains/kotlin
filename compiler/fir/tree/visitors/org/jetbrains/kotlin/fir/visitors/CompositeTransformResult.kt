/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElement

sealed class CompositeTransformResult<out T : Any>() {

    class Single<out T : Any>(val _single: T) : CompositeTransformResult<T>()

    class Multiple<out T : Any>(val _list: List<T>) : CompositeTransformResult<T>()


    companion object {
        fun <T : Any> empty() = CompositeTransformResult.Multiple<T>(emptyList<T>())
        fun <T : Any> single(t: T) = CompositeTransformResult.Single(t)
        fun <T : Any> many(l: List<T>) = CompositeTransformResult.Multiple(l)


    }


    val list: List<T>
        get() = when (this) {
            is CompositeTransformResult.Multiple<*> -> _list as List<T>
            else -> error("!")
        }


    val single: T
        get() = when (this) {
            is CompositeTransformResult.Single<*> -> _single as T
            else -> error("!")
        }

    val isSingle
        get() = this is CompositeTransformResult.Single<*>

    val isEmpty
        get() = this is CompositeTransformResult.Multiple<*> && this.list.isEmpty()


}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : FirElement> T.compose() = CompositeTransformResult.single(this)
