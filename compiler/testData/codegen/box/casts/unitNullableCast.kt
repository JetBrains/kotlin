// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
fun foo() {}

fun bar(): Int? = foo() as? Int

fun box(): String {
    return if (bar() == null) "OK" else "fail"
}
