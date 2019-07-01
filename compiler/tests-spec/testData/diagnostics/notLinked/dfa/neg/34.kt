// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 34
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22379
 */
fun case_1() {
    var x: String? = "..."
    while (x!!.length > 1) {
        x = null
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22379
 */
fun case_3() {
    var x: String? = "..."
    while ((x as String).length > 1) {
        x = null
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22379
 */
fun case_4() {
    var x: Boolean? = true
    while (x!!) {
        x = null
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

// TESTCASE NUMBER: 5
fun case_5() {
    var x: Boolean? = true
    while (true && x!!) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
        x = null
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 6
fun case_6() {
    var x: Boolean? = true
    while (false && x!!) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
        x = null
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 7
fun case_7() {
    var x: Boolean? = true
    while (true || x!!) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
        x = null
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 8
fun case_8() {
    var x: Boolean? = true
    while (!(false && x!!)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
        x = null
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 9
fun case_9() {
    var x: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>"..."<!>
    do {
        x = null
        break
    } while (<!UNREACHABLE_CODE!>x!!.length > 1<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!><!UNSAFE_CALL!>.<!>length
}

// TESTCASE NUMBER: 10
fun case_10() {
    var x: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>"..."<!>
    do {
        x = null
        break
    } while (<!UNREACHABLE_CODE!>(x as String).length > 1<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!><!UNSAFE_CALL!>.<!>length
}

// TESTCASE NUMBER: 11
fun case_11() {
    var x: Boolean? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>true<!>
    do {
        x = null
        break
    } while (<!UNREACHABLE_CODE!>x!!<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 12
fun case_12() {
    var x: Boolean? = true
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
        x = null
        break
    } while (<!UNREACHABLE_CODE!>true && x!!<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 13
fun case_13() {
    var x: Boolean? = true
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
        x = null
        break
    } while (<!UNREACHABLE_CODE!>false && x!!<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 14
fun case_14() {
    var x: Boolean? = true
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
        x = null
        break
    } while (<!UNREACHABLE_CODE!>true || x!!<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 15
fun case_15() {
    var x: Boolean? = true
    do {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
        x = null
        break
    } while (<!UNREACHABLE_CODE!>!(false && x!!)<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

/*
 * TESTCASE NUMBER: 16
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22379, KT-28369
 */
fun case_16() {
    var x: Boolean? = true
    while (x!! && if (true) {x = null; true} else true) {
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 17
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22379, KT-28369
 */
fun case_17() {
    var x: Boolean? = true
    while (x!! && if (true) {x = null; true} else true) {
        break
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}
