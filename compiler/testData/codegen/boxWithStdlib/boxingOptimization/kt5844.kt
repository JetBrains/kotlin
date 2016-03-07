// WITH_RUNTIME

import kotlin.test.assertEquals

fun test1() {
    val u = when (true) {
        true -> 42
        else -> 1.0
    }

    assertEquals(42, u)
}

fun test2() {
    val u = 1L.let {
        when (it) {
            is Long -> if (it.toLong() == 2L) it.toLong() else it * 2L // CompilationException
            else -> it.toDouble()
        }
    }

    assertEquals(2L, u)
}

fun box(): String {
    test1()
    test2()
    return "OK"
}
