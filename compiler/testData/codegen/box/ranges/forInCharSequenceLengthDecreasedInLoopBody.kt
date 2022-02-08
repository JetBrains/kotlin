// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    val sb = StringBuilder("1234")
    val result = StringBuilder()
    for (c in sb) {
        sb.clear()
        result.append(c)
    }
    assertEquals("", sb.toString())
    assertEquals("1", result.toString())

    return "OK"
}