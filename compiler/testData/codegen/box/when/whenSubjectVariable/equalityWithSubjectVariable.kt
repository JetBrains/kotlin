// !LANGUAGE: +VariableDeclarationInWhenSubject

val x = 1

fun box() =
    when (val y = x) {
        1 -> "OK"
        else -> "Fail: $y"
    }