// !LANGUAGE: +VariableDeclarationInWhenSubject
// IGNORE_BACKEND: WASM

val x = 1

fun box() =
    when (val y = x) {
        in 0..2 -> "OK"
        else -> "Fail: $y"
    }
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
