// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// FULL_JDK
import kotlin.test.*

fun box(): String {
    val m = HashMap<String, String>()
    m["a"] = "A"
    m["a"] += "B"

    assertEquals("AB", m["a"])

    return "OK"
}
