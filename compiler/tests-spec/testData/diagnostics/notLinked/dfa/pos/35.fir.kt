// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756
 */
fun case_1(x: Any?) {
    while (true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!> ?: return
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?) {
    while (true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!> ?: return
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756, KT-35668
 */
fun case_3(x: Any?) {
    while (true) {
        x ?: return ?: <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?) {
    while (true) {
        x ?: break
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.equals(10)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    while (true) {
        x ?: continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.equals(10)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    do {
        x ?: continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.equals(10)
    } while (false)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
}

// TESTCASE NUMBER: 7
fun case_7(x: Any?) {
    do {
        x ?: break
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.equals(10)
    } while (false)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
}
