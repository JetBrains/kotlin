// FIR_IDENTICAL
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 9 -> sentence 2
 */

fun foo(x: Int) {
    when (x) {
        2 -> {}
        3 -> {}
    }
}