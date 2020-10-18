// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
fun foo(): Array<Boolean> {
    return arrayOf(
        19 < 20.0,
        12 > 11,
        3.0F <= 4.0,
        4.0F >= 4,
        0.0 / 0 != 0.0 / 0,
        0.0 == -0.0,
        123 == 123,
        123L == 123L,
        0.0F == -0.0F,
        0.0.compareTo(-0.0) > 0,
        (0.0 / 0.0).compareTo(1.0 / 0.0) > 0
    )
}

fun box(): String {
    if (foo().any { it == false })
        return "fail: ${foo().contentDeepToString()}"
    return "OK"
}
