// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 26
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
open class Case1<K : Number> {
    open inner class Case1_1<L>: Case1<Int>() where L : CharSequence {
        inner class Case1_2<M>: Case1<K>.Case1_1<M>() where M : Map<K, L> {
            inline fun <reified T>case_1(x: Any?) {
                x <!UNCHECKED_CAST!>as M<!>
                x <!UNCHECKED_CAST!>as L<!>
                x <!UNCHECKED_CAST!>as K<!>
                if (x is T) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("K & L & M & T!! & kotlin.Any & kotlin.Any?")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("K & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.toByte()
                    <!DEBUG_INFO_EXPRESSION_TYPE("L & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.length
                    <!DEBUG_INFO_EXPRESSION_TYPE("L & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.size
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.isEmpty()
                    <!TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING!><!DEBUG_INFO_EXPRESSION_TYPE("M & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>[null]<!>
                }
            }
        }
    }
}

// TESTCASE NUMBER: 2
inline fun <reified T : CharSequence>case_2(x: Any?) {
    x as T
    if (<!USELESS_IS_CHECK!>x !is T<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
    }
}

// TESTCASE NUMBER: 3
inline fun <reified T : CharSequence>case_3(x: Any?) {
    x as T?
    if (x is T) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!! & kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
    }
}

// TESTCASE NUMBER: 4
inline fun <reified T : CharSequence>case_4(x: Any?) {
    (x as? T)!!
    if (<!USELESS_IS_CHECK!>x is T?<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!! & kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
    }
}

// TESTCASE NUMBER: 5
inline fun <reified T : CharSequence>case_5(x: Any?) {
    if (x as? T != null) {
        if (<!USELESS_IS_CHECK!>x is T?<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T & T!! & kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.length
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.get(0)
        }
    }
}
