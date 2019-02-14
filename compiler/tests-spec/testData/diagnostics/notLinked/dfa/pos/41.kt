// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 41
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-28362
 */
fun case_1(x: Any) {
    if (x is Interface1) {
        if (x is Interface2) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & kotlin.Any")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any"), DEBUG_INFO_SMARTCAST!>x<!>.itest()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any"), DEBUG_INFO_SMARTCAST!>x<!>.itest1()
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any"), DEBUG_INFO_SMARTCAST!>x<!>.itest2()
        }
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-28362
 */
fun case_2(x: Any) {
    if (x is Interface2) {
        if (x is Interface1) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & kotlin.Any")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any"), DEBUG_INFO_SMARTCAST!>x<!>.itest0()
        }
    }
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-28362
 */
fun case_3(x: Any) {
    if (x is Interface1) {
        if (x is Interface2) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & kotlin.Any")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any"), DEBUG_INFO_SMARTCAST!>x<!>.itest000()
        }
    }
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-28362
 */
fun case_4(x: Any) {
    if (x is Interface2) {
        if (x is Interface1) {
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & kotlin.Any")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any"), DEBUG_INFO_SMARTCAST!>x<!>.itest0000()
        }
    }
}
