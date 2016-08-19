object A

fun test(x: Any?) =
        when (x) {
            null -> "null"
            A -> "A"
            is String -> "String"
            in setOf<Nothing>() -> "nothingness?"
            else -> "something"
        }