// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

inline fun <T> Iterable<T>.myForEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}

private fun testMyForEach() {
    val visited = mutableListOf<Pair<Int, Int>>()

    for (i in 1..3) {
        (1..3).myForEach { j ->
            if (j == 3) {
                break
            }
            visited += i to j
        }
    }

    assertEquals(listOf(1 to 1, 1 to 2), visited)
}

inline fun <T> Iterable<T>.myForEachWithBreak(dealBreaker: T, action: (T) -> Unit): Unit {
    for (element in this) {
        if (element == dealBreaker)
            break
        action(element)
    }
}

private fun testMyForEachWithBreak() {
    val visited = mutableListOf<Pair<Int, Int>>()

    for (i in 1..3) {
        (1..3).myForEachWithBreak(3) { j ->
            if (i == 3) {
                break
            }
            visited += i to j
        }
    }

    assertEquals(listOf(1 to 1, 1 to 2, 2 to 1, 2 to 2), visited)
}

inline fun <T> Iterable<T>.myForEachWithContinue(skipper: T, action: (T) -> Unit): Unit {
    for (element in this) {
        if (element == skipper)
            continue
        action(element)
    }
}

private fun testMyForEachWithContinue() {
    val visited = mutableListOf<Pair<Int, Int>>()

    for (i in 1..3) {
        (1..3).myForEachWithContinue(2) { j ->
            if (j == 3) {
                break
            }
            visited += i to j
        }
    }

    assertEquals(listOf(1 to 1), visited)
}

fun box(): String {
    testMyForEach()
    testMyForEachWithBreak()
    testMyForEachWithContinue()
    return "OK"
}