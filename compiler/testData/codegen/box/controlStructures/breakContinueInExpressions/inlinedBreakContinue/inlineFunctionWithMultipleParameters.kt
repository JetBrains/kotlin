// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

inline fun foo(
    block1: () -> Unit,
    noinline block2: () -> Unit,
    block3: () -> Unit
) {
    block1()
    block2()
    block3()
}

fun box(): String {
    val visited = mutableListOf<Int>()

    for (i in 1..3) {
        foo(
            { visited += 1; if (i == 1) continue },
            { visited += 2 },
            { visited += 3; if (i == 2) break },
        )
    }

    assertEquals(listOf(1, 1, 2, 3), visited)

    return "OK"
}