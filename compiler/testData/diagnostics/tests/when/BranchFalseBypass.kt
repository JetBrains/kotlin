enum class My { A, B }

fun test(a: My): String {
    val q: String?

    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (a) {
        My.A -> q = "1"
        My.B -> q = "2"
    }<!>
    // When is exhaustive
    return <!DEBUG_INFO_SMARTCAST!>q<!>
}
