// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions


fun interface KRunnable {
    fun invoke()
}

fun test(a: Any?) {
    a as () -> Unit
    KRunnable(a).invoke()
}

fun box(): String {
    var result = "Fail"
    test {
        result = "OK"
    }
    return result
}
