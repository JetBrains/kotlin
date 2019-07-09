// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 38
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22454
 */
fun case_1() {
    var x: String? = null

    outer@ while (true) {
        inner@ while (x == null) {
            break@outer
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22454
 */
fun case_2() {
    var x: String? = null

    outer@ while (true) {
        inner@ while (x === null) {
            break@outer
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22454
 */
fun case_3(y: Nothing?) {
    var x: String? = null

    outer@ while (true) {
        inner@ while (x === <!DEBUG_INFO_CONSTANT!>y<!>) {
            break@outer
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22454
 */
fun case_4(y: Nothing?) {
    var x: String? = null

    outer1@ while (true) {
        outer2@ while (x == <!DEBUG_INFO_CONSTANT!>y<!>) {
            inner@ while (true) {
                break@outer1
            }
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

// TESTCASE NUMBER: 5
fun case_5(y: Nothing?) {
    var x: String? = null

    outer@ while (x == null) {
        inner@ while (true) {
            break@outer
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!><!UNSAFE_CALL!>.<!>length
}

// TESTCASE NUMBER: 6
fun case_6(y: Nothing?) {
    var x: String? = null

    outer1@ while (true) {
        outer2@ while (x == <!DEBUG_INFO_CONSTANT!>y<!>) {
            inner@ while (true) {
                break@outer2
            }
        }
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!><!UNSAFE_CALL!>.<!>length
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22454
 */
fun case_7() {
    var x: String? = null

    outer@ do {
        inner@ do {
            break@outer
        } while (<!UNREACHABLE_CODE!>x == null<!>)
    } while (<!UNREACHABLE_CODE!>true<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22454
 */
fun case_8(y: Nothing?) {
    var x: String? = null

    outer1@ do {
        outer2@ do {
            inner@ do {
                break@outer1
            } while (<!UNREACHABLE_CODE!>true<!>)
        } while (<!UNREACHABLE_CODE!>x === <!DEBUG_INFO_CONSTANT!>y<!><!>)
    } while (<!UNREACHABLE_CODE!>true<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

// TESTCASE NUMBER: 9
fun case_9() {
    var x: String? = null

    outer@ while (x != null) {
        inner@ do {
            x = null
        } while (x != null)
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.String?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.String?")!>x<!><!UNSAFE_CALL!>.<!>length
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    var x: String? = null

    outer@ while (x != null) {
        inner@ do {
            x = null
        } while (true)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?"), UNREACHABLE_CODE!>x<!>
        <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!><!UNSAFE_CALL!>.<!>length<!>
    }
}

// TESTCASE NUMBER: 11
fun case_11() {
    var x: String? = null

    outer@ while (x != null) {
        inner@ do {
            x = null
            break
        } while (<!UNREACHABLE_CODE!>x == null<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!><!UNSAFE_CALL!>.<!>length
    }
}
