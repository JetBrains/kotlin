object A

fun testWithSubject(x: Any?) =
        when (x) {
            null -> "null"
            A -> "A"
            is String -> "String"
            in setOf<Nothing>() -> "nothingness?"
            else -> "something"
        }

fun test(x: Any?) =
        when {
            x == null -> "null"
            x == A -> "A"
            x is String -> "String"
            x in setOf<Nothing>() -> "nothingness?"
            else -> "something"
        }
