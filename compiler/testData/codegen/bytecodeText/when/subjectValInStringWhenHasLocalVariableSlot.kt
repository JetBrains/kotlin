// !LANGUAGE: +VariableDeclarationInWhenSubject

fun test(a: String) =
    when (val subject = a) {
        "" -> 0
        "a" -> 1
        else -> -1
    }

// 1 LOCALVARIABLE subject