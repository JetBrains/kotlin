// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun describe(x: Int): String {
    return <!NO_ELSE_IN_WHEN!>when(x) {
        1 -> "One"
        2 -> "Two"
    }<!>
}

