// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
//WITH_RUNTIME
import kotlin.test.assertEquals

enum class TestEnumClass(val x: Int) {
    ZERO
//    {
//        init {
//        }
//    }
    ;
    constructor(): this(0)
}

fun box(): String {
    assertEquals(TestEnumClass.ZERO.x, 0)

    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
