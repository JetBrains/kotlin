// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 14
 * DESCRIPTION: Raw data flow analysis test
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
