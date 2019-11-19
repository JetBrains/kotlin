/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 10
 */

enum class E { A, B }

fun foo(e: E, something: Any?): Int {
    if (something != null) return 0

    return when (e) {
        E.A -> 1
        E.B -> 2
        <!DEBUG_INFO_CONSTANT!>something<!> -> 3
    }
}
