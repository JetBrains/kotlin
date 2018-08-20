// !LANGUAGE: +VariableDeclarationInWhenSubject

fun foo(): Any = 1

fun test() =
    when (val y = foo()) {
        42 -> 1
        is String -> y.length
        !is Int -> 2
        in 0..10 -> 3
        !in 10..20 -> 4
        else -> -1
    }