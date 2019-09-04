/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-155, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 9 -> sentence 2
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 *  - expressions, function-literals, lambda-literals -> paragraph 10 -> sentence 1
 */

fun foo(x: Int) {
    r {
        when (x) {
            2 -> <!UNUSED_EXPRESSION!>0<!>
        }
    }
}

fun r(f: () -> Unit) {
    f()
}