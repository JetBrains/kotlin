// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    var x: Any? = null

    if (true) {
        x = 42
    } else {
        x = 42
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>x<!>.inv()
}

// TESTCASE NUMBER: 2
fun case_2() {
    val x: Any?

    if (true) {
        x = 42
    } else {
        x = 42.0
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<kotlin.Int & kotlin.Double> & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Comparable<kotlin.Int & kotlin.Double> & kotlin.Any?")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Any? = null

    if (true) {
        x = ClassLevel2()
    } else {
        x = ClassLevel3()
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 4
fun case_4() {
    val x: Any?

    if (true) {
        return
    } else {
        x = 42.0
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double & kotlin.Any?")!>x<!>.minus(10.0)
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Any?

    if (true) {
        throw Exception()
    } else {
        x = 42.0
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double & kotlin.Any?")!>x<!>.minus(10.0)
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-35668
 */
fun case_6() {
    val x: Any?

    if (true) {
        x = 42.0
    } else {
        null!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), UNINITIALIZED_VARIABLE!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?"), UNINITIALIZED_VARIABLE!>x<!>.<!INAPPLICABLE_CANDIDATE!>minus<!>(10.0)
}
