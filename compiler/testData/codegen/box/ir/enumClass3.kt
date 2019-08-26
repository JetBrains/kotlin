// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
//WITH_RUNTIME
import kotlin.test.assertEquals

enum class TestEnumClass {
    ZERO {
        override fun describe() = "nothing"
    };
    abstract fun describe(): String
}

fun box(): String {
    assertEquals(TestEnumClass.ZERO.describe(), "nothing")
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: ENUM