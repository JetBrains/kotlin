interface A {
    val method : () -> Unit?
}

fun test(a : A) {
    if (a.method != null) {
        a.method!!()
    }
}

class B : A {
    override val method = { }
}

fun box(): String {
    test(B())
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT

