// !LANGUAGE: +VariableDeclarationInWhenSubject

fun test(x: Any?) =
        when (val y = x) {
            is String -> "String, length = ${<!DEBUG_INFO_SMARTCAST!>y<!>.length}"
            null -> "Null"
            else -> "Any, hashCode = ${<!DEBUG_INFO_SMARTCAST!>y<!>.hashCode()}"
        }
