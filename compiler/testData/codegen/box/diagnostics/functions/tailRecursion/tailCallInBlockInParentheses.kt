// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
// DONT_RUN_GENERATED_CODE: JS

tailrec fun foo(x: Int) {
    return if (x > 0) {
        (foo(x - 1))
    }
    else Unit
}

fun box(): String {
    foo(1000000)
    return "OK"
}
