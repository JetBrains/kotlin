// !WITH_NEW_INFERENCE
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 9 -> sentence 1
 * expressions, conditional-expression -> paragraph 4 -> sentence 1
 * expressions, conditional-expression -> paragraph 5 -> sentence 1
 * overload-resolution, determining-function-applicability-for-a-specific-call, rationale -> paragraph 1 -> sentence 1
 * overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 3 -> sentence 1
 */

fun foo(x: Int) = x

fun test0(flag: Boolean) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>if (flag) true else ""<!>)
}

fun test1(flag: Boolean) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>when (flag) {
        true -> true
        else -> ""
    }<!>)
}
