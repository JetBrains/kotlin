// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
fun foo() {}

fun box(): String {
    val x = when ("A") {
        "B" -> foo()
        else -> null
    }

    foo()
    
    return if (x == null) "OK" else "Fail"
}
