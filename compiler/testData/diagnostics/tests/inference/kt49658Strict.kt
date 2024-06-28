// LANGUAGE: +DontLoseDiagnosticsDuringOverloadResolutionByReturnType
// WITH_STDLIB

fun doTheMapThing1(elements: List<CharSequence>): List<String> {
    return elements.<!CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION!>flatMap {
        <!TYPE_MISMATCH!>when (it) { // NullPointerException
            is String -> listOf("Yeah")
            else -> null
        }<!>
    }<!>
}

fun doTheMapThing2(elements: List<CharSequence>): List<String> {
    return elements.<!CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION!>flatMap {
        <!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>if (it is String) listOf("Yeah") else null<!> // it's OK with `if`
    }<!>
}
