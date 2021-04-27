/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 9 -> sentence 2
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 * expressions, function-literals, lambda-literals -> paragraph 10 -> sentence 1
 * overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 3 -> sentence 3
 */

fun foo(x: Int) {
    r {
        <!ARGUMENT_TYPE_MISMATCH!>when (x) {
            2 -> 0
        }<!>
    }
}

fun r(f: () -> Int) {
    f()
}
