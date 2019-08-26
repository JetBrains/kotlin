// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
enum class E {
    A, B;
}

fun foo(e: E?): String {
    val c = when (e) {
        null -> "Fail: null"
        E.B -> "OK"
        E.A -> "Fail: A"
    }
    return c
}

fun box(): String = foo(E.B)

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
