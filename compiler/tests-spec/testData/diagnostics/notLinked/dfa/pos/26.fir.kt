// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
open class Case1<K : Number> {
    open inner class Case1_1<L>: Case1<Int>() where L : CharSequence {
        inner class Case1_2<M>: Case1<K>.Case1_1<M>() where M : Map<K, L> {
            inline fun <reified T>case_1(x: Any?) {
                x as M
                x as L
                x as K
                if (x is T) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & L & K & T!! & kotlin.Any?")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & L & K & T!! & kotlin.Any?")!>x<!>.toByte()
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & L & K & T!! & kotlin.Any?")!>x<!>.length
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & L & K & T!! & kotlin.Any?")!>x<!>.get(0)
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & L & K & T!! & kotlin.Any?")!>x<!>.size
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & L & K & T!! & kotlin.Any?")!>x<!>.isEmpty()
                    <!DEBUG_INFO_EXPRESSION_TYPE("M & L & K & T!! & kotlin.Any?")!>x<!>[null]
                }
            }
        }
    }
}

// TESTCASE NUMBER: 2
inline fun <reified T : CharSequence>case_2(x: Any?) {
    x as T
    if (x !is T) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>.length
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>.get(0)
    }
}

// TESTCASE NUMBER: 3
inline fun <reified T : CharSequence>case_3(x: Any?) {
    x as T?
    if (x is T) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>.length
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>.get(0)
    }
}

// TESTCASE NUMBER: 4
inline fun <reified T : CharSequence>case_4(x: Any?) {
    (x as? T)!!
    if (x is T?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>length<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>get<!>(0)
    }
}

// TESTCASE NUMBER: 5
inline fun <reified T : CharSequence>case_5(x: Any?) {
    if (x as? T != null) {
        if (x is T?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("T?!! & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T?!! & kotlin.Any?")!>x<!>.length
            <!DEBUG_INFO_EXPRESSION_TYPE("T?!! & kotlin.Any?")!>x<!>.get(0)
        }
    }
}
