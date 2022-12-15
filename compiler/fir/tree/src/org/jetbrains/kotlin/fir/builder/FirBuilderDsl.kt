/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.MutableOrEmptyList

@DslMarker
annotation class FirBuilderDsl

fun <T> MutableList<T>.toMutableOrEmpty(): MutableOrEmptyList<T> =
    if (isEmpty()) MutableOrEmptyList.empty() else MutableOrEmptyList(this)

@JvmName("toMutableOrEmptyForImmutable")
fun <T> List<T>.toMutableOrEmpty(): MutableOrEmptyList<T> =
    if (isEmpty()) MutableOrEmptyList.empty() else MutableOrEmptyList(this.toMutableList())
