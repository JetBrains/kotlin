// WITH_STDLIB

import kotlin.test.*

enum class Zzz(val value: String.() -> Int = {
    length
}) {
    Q()
}

fun box(): String {
    assertEquals("Q", Zzz.Q.toString())
    return "OK"
}
