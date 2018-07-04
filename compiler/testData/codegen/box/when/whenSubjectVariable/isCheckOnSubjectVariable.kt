// !LANGUAGE: +VariableDeclarationInWhenSubject
// IGNORE_BACKEND: JS

val x: Any = 1

fun box() =
    when (val y = x) {
        is Int -> "OK"
        else -> "Fail: $y"
    }