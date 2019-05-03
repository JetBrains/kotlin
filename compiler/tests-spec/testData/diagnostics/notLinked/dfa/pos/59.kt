// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 59
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_1() {
    var x: Any? = null
    if (x == null) return
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x != null)
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_2() {
    var x: Any? = null
    if (x === null) return
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x !== null)
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Any? = null
    while (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        x = <!DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    }
}

// TESTCASE NUMBER: 4
fun case_4() {
    var x: Any? = null
    while (x !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        x = <!DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_5() {
    var x: Any? = null
    if (x == null) return
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x !== null)
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_6() {
    var x: Any? = null
    if (x === null) return
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x != null)
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_7() {
    var x: Any? = null
    x ?: return
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x !== null)
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_8() {
    var x: Any? = null
    if (x == null) throw Exception()
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x != null)
}

/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_9() {
    var x: Any? = null
    x ?: throw Exception()
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x !== null)
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_10() {
    var x: Any? = null
    x as Any
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x != null)
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_11() {
    var x: Any? = null
    x <!USELESS_CAST!>as? Any<!> ?: null!!
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x != null)
}

/*
 * TESTCASE NUMBER: 12
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_12() {
    var x: Any? = null
    if (x is Any) {
        do {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            x = x <!UNSAFE_CALL!>.<!>equals(10)
        } while (x != null)
    }
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_13() {
    var x: Any? = null
    if (x != null) {
        do {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            x = x <!UNSAFE_CALL!>.<!>equals(10)
        } while (x != null)
    }
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_14() {
    var x: Any? = null
    if (x == null) return
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        x = x<!UNSAFE_CALL!>.<!>equals(10)
    } while (x is Any)
}

/*
 * TESTCASE NUMBER: 15
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7508
 */
fun case_15() {
    var x: Any? = null
    if (x is Any) {
        do {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            x = x <!UNSAFE_CALL!>.<!>equals(10)
        } while (x is Any)
    }
}