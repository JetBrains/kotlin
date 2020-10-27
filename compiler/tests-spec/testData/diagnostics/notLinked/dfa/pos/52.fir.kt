// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22175
 */
fun case_1(x: String?) = x
fun case_1(x: Int?) = x
fun case_1() {
    var x: Int? = 10
    x = null
    case_1(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>)
}

// TESTCASE NUMBER: 2
fun case_2(x: Int?) = x
fun case_2(x: Nothing?) = x
fun case_2() {
    var x: Int? = 10
    x = null
    case_2(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>)
}

// TESTCASE NUMBER: 3
fun case_3(x: String?) = x
fun case_3() {
    var x: Int? = 10
    x = null
    <!INAPPLICABLE_CANDIDATE!>case_3<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>)
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22175
 */
fun String?.case_4() = this
fun Int?.case_4() = this
fun case_4() {
    var x: Int? = 10
    x = null
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>.case_4()
}

// TESTCASE NUMBER: 5
fun Int?.case_5() = this
fun Nothing?.case_5() = this
fun case_5() {
    var x: Int? = 10
    x = null
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>.case_5()
}

// TESTCASE NUMBER: 6
fun String?.case_6() = this
fun case_6() {
    var x: Int? = 10
    x = null
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>case_6<!>()
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22175
 */
fun <T : String?> T.case_7() = this
fun <T : Int?> T.case_7() = this
fun case_7() {
    var x: Int? = 10
    x = null
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>.case_7()
}

// TESTCASE NUMBER: 8
fun <T : Int?> T.case_8() = this
fun <T : Nothing?> T.case_8() = this
fun case_8() {
    var x: Int? = 10
    x = null
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>.case_8()
}

// TESTCASE NUMBER: 9
fun <T : String?> T.case_9() = this
fun case_9() {
    var x: Int? = 10
    x = null
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>case_9<!>()
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22175
 */
fun <T : String> T?.case_10() = this
fun <T : Int> T?.case_10() = this
fun case_10() {
    var x: Int? = 10
    x = null
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>.case_10()
}

// TESTCASE NUMBER: 11
fun <T : String> T?.case_11() = this
fun case_11() {
    var x: Int? = 10
    x = null
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>case_11<!>()
}
