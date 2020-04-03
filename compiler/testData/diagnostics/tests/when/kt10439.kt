// !WITH_NEW_INFERENCE
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 9 -> sentence 1
 *  - expressions, conditional-expression -> paragraph 4 -> sentence 1
 *  - expressions, conditional-expression -> paragraph 5 -> sentence 1
 *  - overload-resolution, determining-function-applicability-for-a-specific-call, rationale -> paragraph 1 -> sentence 1
 *  - overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 3 -> sentence 1
 */

fun foo(x: Int) = x

fun test0(flag: Boolean) {
    foo(<!NI;TYPE_MISMATCH!>if (flag) <!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!> else <!OI;TYPE_MISMATCH!>""<!><!>)
}

fun test1(flag: Boolean) {
    foo(<!NI;TYPE_MISMATCH!>when (flag) {
        true -> <!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>
        else -> <!OI;TYPE_MISMATCH!>""<!>
    }<!>)
}
