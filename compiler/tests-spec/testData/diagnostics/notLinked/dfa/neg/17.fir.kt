// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 17
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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & Interface1 & Interface2")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & Interface1 & Interface2")!>x<!>.itest00()
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
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & Interface2 & Interface1")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & Interface2 & Interface1")!>x<!>.itest00000()
        }
    }
}
