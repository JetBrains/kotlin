/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 6
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 7
 *  - expressions, when-expression -> paragraph 9 -> sentence 2
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 3 -> sentence 1
 *  - type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 */

sealed class S

object First : S()

class Derived(val s: String) : S()

object Last : S()

fun use(s: String) = s

fun foo(s: S) {
    <!NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS!>when<!> (s) {
        First -> {}
        is Derived -> use(<!DEBUG_INFO_SMARTCAST!>s<!>.s)
    }
}