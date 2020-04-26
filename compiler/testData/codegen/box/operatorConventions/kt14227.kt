// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
import kotlin.test.*

fun box(): String {
    val m = HashMap<String, String>()
    m["a"] = "A"
    m["a"] += "B"

    assertEquals("AB", m["a"])

    return "OK"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_HASH_MAP
