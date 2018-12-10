// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val m = HashMap<String, String>()
    m["a"] = "A"
    m["a"] += "B"

    assertEquals("AB", m["a"])

    return "OK"
}
