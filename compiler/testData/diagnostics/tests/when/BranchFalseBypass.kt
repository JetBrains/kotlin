/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: pos):
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 *  - expressions, when-expression -> paragraph 6 -> sentence 1
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 *  - type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 8
 */
enum class My { A, B }

fun test(a: My): String {
    val q: String?

    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (a) {
        My.A -> q = "1"
        My.B -> q = "2"
    }<!>
    // When is exhaustives
    return <!DEBUG_INFO_SMARTCAST!>q<!>
}
