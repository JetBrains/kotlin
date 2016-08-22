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

fun testComma(x: Int) =
        when (x) {
            1, 2, 3, 4 -> "1234"
            5, 6, 7 -> "567"
            8, 9 -> "89"
            else -> "?"
        }