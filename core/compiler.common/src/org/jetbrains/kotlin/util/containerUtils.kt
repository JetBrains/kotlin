/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

/**
 * Perform BFS on the given collection with neighbors created by the given function.
 */
fun <T> Collection<T>.bfs(getNeighbors: (T) -> Iterator<T>): Sequence<T> {
    val queue = ArrayDeque(this)
    val visited = mutableSetOf<T>()
    return sequence {
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in visited) continue
            visited.add(current)
            yield(current)
            getNeighbors(current).forEach(queue::add)
        }
    }
}
