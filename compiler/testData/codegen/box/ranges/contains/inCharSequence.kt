// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val charSeq: String = "123"

fun box(): String = when {
    '0' in charSeq -> "fail 1"
    '1' !in charSeq -> "fail 2"
    else -> "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_TEXT
