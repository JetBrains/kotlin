// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

class C {
    companion object {
        val visited = mutableListOf<Int>()

        init {
            for (i in 1..5) {
                run {
                    if (i == 2) continue
                    if (i == 4) break
                }
                C.visited.add(i)
            }
        }
    }

    val visited = mutableListOf<Int>()

    init {
        for (i in 1..5) {
            run {
                if (i == 1) continue
                if (i == 4) break
            }
            visited.add(i)
        }
    }
}

fun box(): String {
    val c = C()
    assertEquals(listOf(1, 3), C.visited)
    assertEquals(listOf(2, 3), c.visited)
    return "OK"
}
