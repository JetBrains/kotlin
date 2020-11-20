/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import java.lang.ref.WeakReference

class OneElementWeakMap<K, V> private constructor(
    private val keyReference: WeakReference<K>,
    private val valueReference: WeakReference<V>
) {
    constructor(key: K, value: V) : this(WeakReference(key), WeakReference(value))

    val key: K?
        get() = keyReference.get()

    val value: V?
        get() = valueReference.get()
}
