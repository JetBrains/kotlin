// DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 32
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun <T: Any, K: Any> case_1(x: T?, y: K?) {
    x as T
    y as K
    val z = <!DEBUG_INFO_EXPRESSION_TYPE("T? & Any")!>x<!> <!USELESS_ELVIS!>?: <!DEBUG_INFO_EXPRESSION_TYPE("K? & Any")!>y<!><!>

    <!DEBUG_INFO_EXPRESSION_TYPE("T? & Any")!>x<!>.equals(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>z<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>z<!>.equals(10)
}

// TESTCASE NUMBER: 1
inline fun <reified T: Any, reified K: T> case_2(y: K?) {
    y as K

    <!DEBUG_INFO_EXPRESSION_TYPE("K? & Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("K? & Any")!>y<!>.equals(10)
}
