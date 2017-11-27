// WITH_RUNTIME

import kotlin.test.assertEquals

fun box() : String {
    val a = Pair(1, 2)
    when (a) {
        match (_, d) -> {
            assertEquals(d, 2)
            return "OK"
        }
        else -> return "match fail"
    }
    return "fail when generation"
}