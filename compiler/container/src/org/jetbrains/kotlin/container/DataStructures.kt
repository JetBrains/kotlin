/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

class CycleInTopoSortException : Exception()
