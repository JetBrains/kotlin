// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 4 -> sentence 1
 * RELEVANT PLACES:
 *      paragraph 1 -> sentence 2
 *      paragraph 6 -> sentence 1
 *      paragraph 9 -> sentence 3
 *      paragraph 9 -> sentence 4
 * NUMBER: 14
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality) using if expression and vararg.
 */

// TESTCASE NUMBER: 1
fun case_1(vararg x: Int?) {
    if (<!SENSELESS_COMPARISON!>x != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<out kotlin.Int?>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x[0]<!>
    }
}

// TESTCASE NUMBER: 2
fun case_2(vararg x: Int?) {
    x[0].apply {
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), DEBUG_INFO_SMARTCAST!>this<!>.inv()
        }
    }

    x[0].also {
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>it<!>.inv()
        }
    }
}

// TESTCASE NUMBER: 3
fun <T> case_3(vararg x: T?) {
    if (<!SENSELESS_COMPARISON!>x != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<out T?>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T?")!>x[0]<!>
    }
}

// TESTCASE NUMBER: 4
fun <T : Number?> case_4(vararg x: T?) {
    x[0].apply {
        if (this != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_EXPRESSION_TYPE("T?")!>this<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_EXPRESSION_TYPE("T?"), DEBUG_INFO_SMARTCAST!>this<!>.toByte()
        }
    }

    x[0].also {
        if (it != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>it<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>it<!>.toByte()
        }
    }
}
