// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val progression = 1 .. 3 step 2

fun box(): String = when {
    0 in progression -> "fail 1"
    1 !in progression -> "fail 2"
    else -> "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_COLLECTIONS
