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
    foo(<!TYPE_MISMATCH{NI}!>if (flag) <!CONSTANT_EXPECTED_TYPE_MISMATCH{OI}!>true<!> else <!TYPE_MISMATCH{OI}!>""<!><!>)
}

fun test1(flag: Boolean) {
    foo(<!TYPE_MISMATCH{NI}!>when (flag) {
        true -> <!CONSTANT_EXPECTED_TYPE_MISMATCH{OI}!>true<!>
        else -> <!TYPE_MISMATCH{OI}!>""<!>
    }<!>)
}
