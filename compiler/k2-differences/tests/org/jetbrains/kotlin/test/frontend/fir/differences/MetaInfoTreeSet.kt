/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import java.util.*
import kotlin.NoSuchElementException

typealias MetaInfoSet = MapBasedSetWithCustomKey<Any, ParsedCodeMetaInfo>

fun metaInfoSet(vararg elements: ParsedCodeMetaInfo) =
    MetaInfoSet { it.equivalenceClass }.also { it.addAll(elements) }

class MetaInfoTreeSet : AbstractMutableSet<ParsedCodeMetaInfo>() {
    private val lookup = TreeMap<Int, MetaInfoSet>()

    override var size = 0
        private set

    override fun add(element: ParsedCodeMetaInfo): Boolean {
        val diagnosticsAtThisStart = lookup[element.start]

        if (diagnosticsAtThisStart == null) {
            lookup[element.start] = metaInfoSet(element)
            size++
            return true
        }

        if (diagnosticsAtThisStart.add(element)) {
            size++
            return true
        }

        return false
    }

    override fun clear() = lookup.clear().also { size = 0 }

    override fun contains(element: ParsedCodeMetaInfo) = lookup[element.start]?.contains(element) == true

    override fun remove(element: ParsedCodeMetaInfo): Boolean {
        val diagnosticsAtThisStart = lookup[element.start] ?: return false

        return diagnosticsAtThisStart.remove(element).also {
            if (diagnosticsAtThisStart.isEmpty()) {
                lookup.remove(element.start)
            }
            size--
        }
    }

    override fun toString() = "MetaInfoTreeSet${lookup.values}"

    inner class Iterator : MutableIterator<ParsedCodeMetaInfo> {
        private val outerDelegate = lookup.iterator()
        private var innerDelegate = advanceInnerDelegate()

        private fun advanceInnerDelegate() = when {
            outerDelegate.hasNext() -> outerDelegate.next().value.iterator()
            else -> null
        }

        override fun hasNext() = innerDelegate?.hasNext() == true || outerDelegate.hasNext()

        override fun next(): ParsedCodeMetaInfo {
            if (innerDelegate?.hasNext() == false) {
                innerDelegate = advanceInnerDelegate()
            }

            return innerDelegate?.next() ?: throw NoSuchElementException()
        }

        override fun remove() {
            innerDelegate?.remove()
        }
    }

    override fun iterator() = Iterator()

    fun hasDiagnosticsAt(start: Int, end: Int) = lookup[start]?.any { it.end == end } == true
}
