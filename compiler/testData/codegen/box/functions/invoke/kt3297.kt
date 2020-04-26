//KT-3297 Calling the wrong function inside an extension method to the Function0 class

infix fun <R> Function0<R>.or(alt: () -> R): R {
    try {
        return this()
    } catch (e: Exception) {
        return alt()
    }
}

fun box(): String {
    return {
        throw RuntimeException("fail")
    } or {
        "OK"
    }
}
// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
