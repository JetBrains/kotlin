fun foo() {}

fun box(): String {
    val x = when ("A") {
        "B" -> foo()
        else -> null
    }

    foo()
    
    return if (x == null) "OK" else "Fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT