// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-31053
 */
fun case_1(x: Any?) {
    if (x !is Nothing?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-31053
 */
fun case_2(x: Pair<*, *>?) {
    if (x is Nothing?) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *>?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-31053
 */
fun case_3(x: Any?) {
    if (x is Nothing?) throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-31053
 */
fun case_4(x: Pair<*, *>?) {
    when (x) {
        is Nothing? -> return
        else -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *>?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *>?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *>?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-31053
 */
fun case_5(x: Pair<*, *>?) {
    when (x) {
        !is Nothing? -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *> & kotlin.Pair<*, *>?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *> & kotlin.Pair<*, *>?")!>x<!>.equals(10)
        }
        else -> return
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *> & kotlin.Pair<*, *>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<*, *> & kotlin.Pair<*, *>?")!>x<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-31053
 */
fun case_6(x: Any?) {
    when (x) {
        is Nothing? -> return
        is Any? -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}
