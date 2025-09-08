// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 28
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun <T : List<T>> Inv<out T>.case_1() {
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>this is MutableList<*><!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & Inv<out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & Inv<out T>")!>this<!>[0] = <!MEMBER_PROJECTED_OUT!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<*> & Inv<out T>")!>this<!>[1]<!>
    }
}
