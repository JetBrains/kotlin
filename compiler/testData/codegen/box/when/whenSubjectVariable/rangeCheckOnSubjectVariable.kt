// !LANGUAGE: +VariableDeclarationInWhenSubject

val x = 1

fun box() =
    when (val y = x) {
        in 0..2 -> "OK"
        else -> "Fail: $y"
    }