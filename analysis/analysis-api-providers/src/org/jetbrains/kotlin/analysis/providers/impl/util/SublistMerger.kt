/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl.util

import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.partitionIsInstance

/**
 * With each call to [merge], [SublistMerger] can merge all elements of a specific (reified) type into a single element using a supplied
 * constructor and then add it to [destination]. Unmerged elements are added to [destination] using [finish].
 *
 * The purpose of [SublistMerger] is to merge multiple different types of elements from a single origin list without the need for
 * intermediate list management and [partitionIsInstance] boilerplate.
 */
public class SublistMerger<A : Any>(
    initialElements: List<A>,
    public val destination: MutableList<A>,
) {
    public var remainingElements: List<A> = initialElements

    public inline fun <reified R : A> merge(create: (List<R>) -> A?) {
        val (specificElements, remainingElements) = this.remainingElements.partitionIsInstance<_, R>()
        destination.addIfNotNull(create(specificElements))
        this.remainingElements = remainingElements
    }

    public fun finish() {
        destination.addAll(remainingElements)
        remainingElements = emptyList()
    }
}

public fun <A : Any> List<A>.mergeInto(destination: MutableList<A>, f: SublistMerger<A>.() -> Unit) {
    SublistMerger(this, destination).apply {
        f()
        finish()
    }
}

public fun <A : Any> List<A>.mergeWith(f: SublistMerger<A>.() -> Unit): List<A> =
    mutableListOf<A>().also { destination -> mergeInto(destination, f) }

public inline fun <A : Any, reified R : A> List<A>.mergeOnly(crossinline create: (List<R>) -> A?): List<A> =
    mergeWith { merge<R>(create) }
