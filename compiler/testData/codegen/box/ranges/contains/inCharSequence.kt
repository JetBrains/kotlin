// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// WITH_STDLIB

val charSeq: String = "123"

fun box(): String = when {
    '0' in charSeq -> "fail 1"
    '1' !in charSeq -> "fail 2"
    else -> "OK"
}