// LANGUAGE: +DontLoseDiagnosticsDuringOverloadResolutionByReturnType
// WITH_STDLIB

fun doTheMapThing1(elements: List<CharSequence>): List<String> {
    return elements.flatMap {
        <!ARGUMENT_TYPE_MISMATCH!>when (it) { // NullPointerException
            is String -> listOf("Yeah")
            else -> null
        }<!>
    }
}

fun doTheMapThing2(elements: List<CharSequence>): List<String> {
    return elements.flatMap {
        <!ARGUMENT_TYPE_MISMATCH!>if (it is String) listOf("Yeah") else null<!> // it's OK with `if`
    }
}
