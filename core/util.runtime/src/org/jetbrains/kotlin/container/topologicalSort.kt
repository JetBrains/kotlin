/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container

import java.util.ArrayList
import java.util.HashSet

fun <T> topologicalSort(items: Iterable<T>, reverseOrder: Boolean = false, dependencies: (T) -> Iterable<T>): List<T> {
    val itemsInProgress = HashSet<T>()
    val completedItems = HashSet<T>()
    val result = ArrayList<T>()

    fun DfsVisit(item: T) {
        if (item in completedItems)
            return

        if (item in itemsInProgress)
            return //throw CycleInTopoSortException()

        itemsInProgress.add(item)

        for (dependency in dependencies(item)) {
            DfsVisit(dependency)
        }

        itemsInProgress.remove(item)
        completedItems.add(item)
        result.add(item)
    }

    for (item in items)
        DfsVisit(item)

    return result.apply { if (!reverseOrder) reverse() }
}
