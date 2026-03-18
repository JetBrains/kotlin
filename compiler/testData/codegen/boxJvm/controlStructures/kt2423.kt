// TARGET_BACKEND: JVM

// WITH_STDLIB
// FULL_JDK

import java.util.LinkedList

fun ok1(): Boolean {
    val queue = LinkedList(listOf(1, 2, 3))
    while (!queue.isEmpty()) {
        queue.poll()
        for (y in 1..3) {
            if (queue.contains(y)) {
                return true
            }
        }
    }
    return false
}

fun ok2(): Boolean {
    val queue = LinkedList(listOf(1, 2, 3))
    val array = arrayOf(1, 2, 3)
    while (!queue.isEmpty()) {
        queue.poll()
        for (y in array) {
            if (queue.contains(y)) {
                return true
            }
        }
    }
    return false
}

fun ok3(): Boolean {
    val queue = LinkedList(listOf(1, 2, 3))
    while (!queue.isEmpty()) {
        queue.poll()
        var x = 0
        do {
            x++
            if (x == 2) return true
        } while (x < 2)
    }
    return false
}

fun box(): String {
    if (!ok1()) return "Fail #1"
    if (!ok2()) return "Fail #2"
    if (!ok3()) return "Fail #3"
    return "OK"
}
