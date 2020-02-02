// !LANGUAGE: +VariableDeclarationInWhenSubject
// IGNORE_BACKEND: WASM

val x: Any = 1

fun box() =
    when (val y = x) {
        is Int -> "OK"
        else -> "Fail: $y"
    }