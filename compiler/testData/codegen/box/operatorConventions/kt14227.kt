// WITH_STDLIB
// FULL_JDK
import kotlin.test.*

fun box(): String {
    val m = HashMap<String, String>()
    m["a"] = "A"
    m["a"] += "B"

    assertEquals("AB", m["a"])

    return "OK"
}
