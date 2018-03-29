/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package runtime.workers.freeze1

import kotlin.test.*

import konan.worker.*

data class Node(var previous: Node?, var data: Int)

fun makeCycle(count: Int): Node {
    val first = Node(null, 0)
    var current = first
    for (index in 1 .. count - 1) {
        current = Node(current, index)
    }
    first.previous = current
    return first
}

data class Node2(var leaf1: Node2?, var leaf2: Node2?)

fun makeDiamond(): Node2 {
    val bottom = Node2(null, null)
    val mid1prime = Node2(bottom, null)
    val mid1 = Node2(mid1prime, null)
    val mid2 = Node2(bottom, null)
    return Node2(mid1, mid2)
}

@Test fun runTest() {
    makeCycle(10).freeze()

    // Must be able to freeze diamond shaped graph.
    val diamond = makeDiamond().freeze()

    val immutable = Node(null, 4).freeze()
    try {
        immutable.data = 42
    } catch (e: InvalidMutabilityException) {
        println("OK, cannot mutate frozen")
    }
}