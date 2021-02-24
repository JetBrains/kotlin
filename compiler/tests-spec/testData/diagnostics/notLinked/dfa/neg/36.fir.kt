// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-13650
 */
fun case_1(x: Class?, y: Any) {
    x?.prop_12 = if (y is String) "" else throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.<!UNRESOLVED_REFERENCE!>toUpperCase<!>()
}

// TESTCASE NUMBER: 2
fun case_2(x: Class?, y: Any) {
    x?.prop_9 = y is String || return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.<!UNRESOLVED_REFERENCE!>toUpperCase<!>()
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-13650
 */
fun case_3(x: Class?, y: Any) {
    x?.prop_12 = y as String
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.<!UNRESOLVED_REFERENCE!>toUpperCase<!>()
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-13650
 */
fun case_4(x: Class?, y: Any) {
    x?.prop_12 = y as? String ?: return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.<!UNRESOLVED_REFERENCE!>toUpperCase<!>()
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-13650
 */
fun case_5(x: Class?, y: String?) {
    x?.prop_12 = y ?: return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>toUpperCase<!>()
}

// TESTCASE NUMBER: 6
fun case_6(x: Class?, y: String?) {
    x?.prop_9 = y !is String && throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>toUpperCase<!>()
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-13650
 */
fun case_7(x: Class?, y: String?) {
    x?.prop_12 = y!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>toUpperCase<!>()
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-13650
 */
fun case_8(x: Class?, y: String?) {
    x?.prop_12 = if (y === null) throw Exception() else ""
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>toUpperCase<!>()
}
