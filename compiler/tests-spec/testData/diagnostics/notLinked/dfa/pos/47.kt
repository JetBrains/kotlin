// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 47
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(a: Any?) {
    while (true) {
        if (a == null) return
        if (true) break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
fun case_2(a: Any?) {
    while (true) {
        if (a == null) continue
        if (true) break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 3
fun case_3(a: Any?) {
    while (true) {
        if (a == null) <!UNREACHABLE_CODE!>return<!> continue
        if (true) break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(10)
}

// TESTCASE NUMBER: 4
fun case_4(a: Any?) {
    while (true) {
        if (a == null) throw Exception()
        break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(10)
}

// TESTCASE NUMBER: 5
fun case_5(a: Any?) {
    while (true) {
        if (a == null) <!UNREACHABLE_CODE!>return<!> throw Exception()
        if (true) break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(10)
}

// TESTCASE NUMBER: 6
fun case_6(a: Any?) {
    while (true) {
        if (a == null) <!UNREACHABLE_CODE!>return<!> throw Exception()
        break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(10)
}
