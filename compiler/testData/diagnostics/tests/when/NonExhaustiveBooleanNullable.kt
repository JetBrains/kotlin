/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 10
 */

// See also: KT-3743
fun foo(arg: Boolean?): String {
    // Must be NOT exhaustive
    return <!NO_ELSE_IN_WHEN!>when<!>(arg) {
        true -> "truth"
        false -> "falsehood"
    }
}
