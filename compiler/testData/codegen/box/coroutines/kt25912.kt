// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// IGNORE_BACKEND: NATIVE

import helpers.*
import COROUTINES_PACKAGE.*

fun box(): String {
    val gg = object : Grouping<Int, String> {
        override fun sourceIterator(): Iterator<Int> = listOf(1).iterator()
        override fun keyOf(element: Int): String = "OK"
    }

    var res = ""
    suspend {
        for (e in gg.sourceIterator()) {
            val key = gg.keyOf(e)
            res += key
        }
    }.startCoroutine(EmptyContinuation)
    return res
}
