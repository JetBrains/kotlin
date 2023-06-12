// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 24
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun <T> case_1(x: Any?) where T: CharSequence {
    x <!UNCHECKED_CAST!>as T<!>
    class Case1<K> where K : T {
        inline fun <reified T : Number> case_1() {
            if (x is T) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T & T")!>x<!>.toByte()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T & T")!>x<!>.length
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T & T")!>x<!>.get(0)
            }
        }
    }
}
