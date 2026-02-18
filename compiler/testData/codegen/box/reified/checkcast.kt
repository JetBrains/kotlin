
// WITH_STDLIB

// FILE: lib.kt
inline fun<reified T> checkcast(x: Any?): T {
    return x as T
}

// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    val x = checkcast<String>("abc")
    assertEquals("abc", x)
    val y = checkcast<Int>(1)
    assertEquals(1, y)

    try {
        val z = checkcast<Int>("abc")
    } catch (e: Exception) {
        return "OK"
    }

    return "Fail"
}
