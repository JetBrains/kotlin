// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
//WITH_RUNTIME

import kotlin.test.assertEquals

enum class Test {
    OK
}

fun box(): String {
    assertEquals(Test.OK.ordinal, 0)
    assertEquals(Test.OK.name, "OK")
    
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
