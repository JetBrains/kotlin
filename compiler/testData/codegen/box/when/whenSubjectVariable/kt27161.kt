// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
enum class Test {
    A, B, OTHER
}

fun peek() = Test.A

fun box(): String {
    val x = when (val type = peek()) {
        Test.A -> "OK"
        else -> "other"
    }
    return x
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
