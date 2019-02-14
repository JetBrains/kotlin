// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 21
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28508
 */
class Case1 {
    val x: Int?
    init {
        val y = this
        if (y.x != null) {
            x = null
            <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>this.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>y.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>y.x<!>.inv()
        } else {
            x = 10
        }
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28508
 */
class Case2 {
    val x: Int
    init {
        val y = this
        if (<!SENSELESS_COMPARISON!>y.x == null<!>) {
            x = 11
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>this.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Nothing")!>y.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Nothing")!>y.x<!>.inv()
            println(1)
        } else {
            x = 11
        }
    }
}
