/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.util

private open class FrozenReversedListReadOnly<out T>(
    private val delegate: List<T>
) : AbstractList<T>() {
    override val size: Int = delegate.size
    override fun get(index: Int): T = delegate[size - 1 - index]
}

fun <T> List<T>.asReversedFrozen(): List<T> = FrozenReversedListReadOnly(this)
