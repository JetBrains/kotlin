// !LANGUAGE: +VariableDeclarationInWhenSubject

fun test(a: Any) =
    when (val subject = a) {
        is Int -> "Int $subject"
        is String -> "String ${subject.length}"
        else -> "other"
    }

// 1 LOCALVARIABLE subject Ljava/lang/Object;