// FIR_IDENTICAL
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-313, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 4
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 5
 */

fun foo(arg: Boolean): String {
    // Must be exhaustive
    return when(arg) {
        (true) -> "truth"
        ((false)) -> "falsehood"
    }
}
