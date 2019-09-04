/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-155, test type: pos):
 *  - expressions, when-expression -> paragraph 2 -> sentence 1
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 */
fun foo(x: Int, y: Int): Int =
        when {
            x > 0<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> y > 0<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!><!SYNTAX!>,<!> x < 0 -> 1
            else -> 0
        }

fun bar(x: Int): Int =
        when (x) {
            0 -> 0
            else -> 1
        }