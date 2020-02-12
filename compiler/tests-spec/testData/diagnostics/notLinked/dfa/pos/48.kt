// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 48
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30317
 */
fun case_1(x: Any?, y: Any?) {
    if (x!! === y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30317
 */
fun case_2(x: Any?, y: Any?) {
    if (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>x as Int === y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>y<!>.inv(<!TOO_MANY_ARGUMENTS!>10<!>)
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30317
 */
fun case_3(x: Any?, y: Any?) {
    if (y === x!!) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30317
 */
fun case_4(x: Any?, y: Any?) {
    if (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>y === x as Int<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>y<!>.inv(<!TOO_MANY_ARGUMENTS!>10<!>)
    }
}
