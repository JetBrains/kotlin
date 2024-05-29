/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.acceptLite

internal class BirElementAncestorsIterator(
    initial: BirElementBase?,
) : Iterator<BirElementBase> {
    private var next: BirElementBase? = initial

    override fun hasNext(): Boolean = next != null

    override fun next(): BirElementBase {
        val n = next!!
        next = n.parent
        return n
    }
}

internal class BirElementAncestorsSequence(private val element: BirElementBase?) : Sequence<BirElementBase> {
    override fun iterator(): Iterator<BirElementBase> = BirElementAncestorsIterator(element)
}

fun BirElement.ancestors(includeSelf: Boolean = false): Sequence<BirElementBase> =
    BirElementAncestorsSequence(if (includeSelf) this as BirElementBase else parent)

fun BirElement.collectAllElementsInTree(): List<BirElement> {
    val list = ArrayList<BirElement>()
    accept {
        list += it
        it.walkIntoChildren()
    }
    return list
}

fun BirElement.countAllElementsInTree(): Int {
    var count = 0
    acceptLite {
        count++
        it.walkIntoChildren()
    }
    return count
}
