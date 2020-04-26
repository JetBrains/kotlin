// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
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
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_STRING_BUILDER
