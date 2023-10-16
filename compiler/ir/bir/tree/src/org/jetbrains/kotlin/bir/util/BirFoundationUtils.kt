/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.BirElement

internal class BirElementAncestorsIterator(
    initial: BirElement?,
) : Iterator<BirElement> {
    private var next: BirElement? = initial

    override fun hasNext(): Boolean = next != null

    override fun next(): BirElement {
        val n = next!!
        next = n.parent
        return n
    }
}

internal class BirElementAncestorsSequence(private val element: BirElement?) : Sequence<BirElement> {
    override fun iterator(): Iterator<BirElement> = BirElementAncestorsIterator(element)
}

fun BirElement.ancestors(includeSelf: Boolean = false): Sequence<BirElement> =
    BirElementAncestorsSequence(if (includeSelf) this else parent)