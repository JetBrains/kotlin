// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 33
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1() {
    var x: Any? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    if (true) {
        x = 42
    } else {
        x = 42
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

// TESTCASE NUMBER: 2
fun case_2() {
    val x: Any?

    if (true) {
        x = 42
    } else {
        x = 42.0
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Any? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>

    if (true) {
        x = ClassLevel2()
    } else {
        x = ClassLevel3()
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
}

// TESTCASE NUMBER: 4
fun case_4() {
    val x: Any?

    if (true) {
        return
    } else {
        x = 42.0
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Double")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Double"), DEBUG_INFO_SMARTCAST!>x<!>.minus(10.0)
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Any?

    if (true) {
        throw Exception()
    } else {
        x = 42.0
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Double")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Double"), DEBUG_INFO_SMARTCAST!>x<!>.minus(10.0)
}

// TESTCASE NUMBER: 6
fun case_6() {
    val x: Any?

    if (true) {
        x = 42.0
    } else {
        null!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Double")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Double"), DEBUG_INFO_SMARTCAST!>x<!>.minus(10.0)
}
