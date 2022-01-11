// WITH_STDLIB

fun doTheMapThing1(elements: List<CharSequence>): List<String> {
    return elements.flatMap {
        <!TYPE_MISMATCH_WARNING!>when (it) { // NullPointerException
            is String -> listOf("Yeah")
            else -> null
        }<!>
    }
}

fun doTheMapThing2(elements: List<CharSequence>): List<String> {
    return elements.flatMap {
        <!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH_WARNING!>if (it is String) listOf("Yeah") else null<!> // it's OK with `if`
    }
}
