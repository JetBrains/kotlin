// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 29
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(a: Any?) {
    while (true) {
        if (a == null) break
        if (true) continue
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 2
fun case_2(a: Any?) {
    (l@ {
        while (true) {
            if (a == null) return@l
            if (true) break
        }
    })()

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 3
fun case_3(a: Any?) {
    loop1@ while (true) {
        loop2@ while (true) {
            if (true) break
            if (a == null) <!UNREACHABLE_CODE!>return<!> continue@loop1
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), UNREACHABLE_CODE!>a<!>
    <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!><!UNSAFE_CALL!>.<!>equals(10)<!>
}

// TESTCASE NUMBER: 4
fun case_4(a: Any?) {
    loop1@ while (true) {
        loop2@ while (true) {
            break@loop1
            <!UNREACHABLE_CODE!>if (a == null) return<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!><!UNSAFE_CALL!>.<!>equals(10)
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 */
fun case_5(a: Any?) {
    loop1@ while (true) {
        loop2@ while (true) {
            <!UNREACHABLE_CODE!>return<!> break@loop1
            <!UNREACHABLE_CODE!>if (a == null) return<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 */
fun case_6(a: Any?) {
    loop1@ while (true) {
        loop2@ while (true) {
            throw break@loop1
            <!UNREACHABLE_CODE!>if (a == null) return<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 */
fun case_7(<!UNUSED_PARAMETER!>a<!>: Any?) {
    loop1@ while (true) {
        loop2@ while (true) {
            throw continue@loop1
            <!UNREACHABLE_CODE!>if (a == null) return<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), UNREACHABLE_CODE!>a<!>
    <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(10)<!>
}

// TESTCASE NUMBER: 8
fun case_8(a: Any?) {
    var b: Any? = 10
    loop1@ while (b != null) {
        loop2@ while (true) {
            b = null
            <!UNREACHABLE_CODE!>return<!> continue@loop1
            <!UNREACHABLE_CODE!>if (a == null) return<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!><!UNSAFE_CALL!>.<!>equals(10)
}
