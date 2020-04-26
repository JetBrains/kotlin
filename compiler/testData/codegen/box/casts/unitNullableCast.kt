fun foo() {}

fun bar(): Int? = foo() as? Int

fun box(): String {
    return if (bar() == null) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT
