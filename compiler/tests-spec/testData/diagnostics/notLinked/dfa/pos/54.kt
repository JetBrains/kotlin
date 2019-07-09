// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 54
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7676
 */
fun case_1() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        while (<!USELESS_IS_CHECK!>a is String<!>) {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7676
 */
fun case_2() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        while (true) {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), UNREACHABLE_CODE!>b<!>
        <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7676
 */
fun case_3() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        do {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        } while (true)

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), UNREACHABLE_CODE!>b<!>
        <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!><!>
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7676
 */
fun case_4() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        do {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        } while (false)

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7676
 */
fun case_5() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        for (i in 0..10) {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        if (<!USELESS_IS_CHECK!>a is String<!>) {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        when (true) {
            true -> b = a
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7676
 */
fun case_8() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        try {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        } catch (e: Exception) {  }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

// TESTCASE NUMBER: 9
fun case_9() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        try {

        } catch (e: Exception) {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        try {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        } finally {

        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
    }
}

// TESTCASE NUMBER: 11
fun case_11() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        try {

        } finally {
            b = a
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
    }
}

/*
 * TESTCASE NUMBER: 12
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7676
 */
fun case_12() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        while (if (true) { b = a; true } else true) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!>
        }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

// TESTCASE NUMBER: 13
fun case_13() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        <!DEBUG_INFO_SMARTCAST!>a<!>.plus(if (true) { b = a; true } else true)

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
    }
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7676
 */
fun case_14() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        while (true) {
            if (true) { b = a; } else 3
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

// TESTCASE NUMBER: 15
fun case_15() {
    var a: Any = 4
    if (a is String) {
        var b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        while (true) {
            if (true) { b = a; } else { b = a; }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>b<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>b<!>.length
        }
    }
}
