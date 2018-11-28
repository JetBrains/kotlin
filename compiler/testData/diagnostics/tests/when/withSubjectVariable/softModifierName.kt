// !LANGUAGE: +VariableDeclarationInWhenSubject

fun test(data: String) =
    when (data.length) {
        0 -> -1
        else -> 42
    }