// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val x = (10L..50).map { it * 40L }
    assertEquals(400L, x.first())
    return "OK"
}




// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
