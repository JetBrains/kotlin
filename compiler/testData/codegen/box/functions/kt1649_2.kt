// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
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
