// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 23
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
class Case1<T : Number> {
    inline fun <reified T : CharSequence>case_1(x: Any?) {
        if (x is T) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.length
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
        }
    }
}

// TESTCASE NUMBER: 2
fun <T> case_2() where T: CharSequence, T: Number {
    class Case1<K> where K : T {
        inline fun <reified T : K> case_1(x: Any?) {
            if (x is T) {
                <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any & kotlin.Any?")!>x<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.toByte()
                <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.length
                <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
            }
        }
    }
}

// TESTCASE NUMBER: 3
fun <T> case_3(x: Any?) where T: CharSequence, T: Number {
    class Case1<K> where K : T {
        inline fun <reified T : K> case_1() {
            if (x is T) {
                <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any & kotlin.Any?")!>x<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.toByte()
                <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.length
                <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
            }
        }
    }
}