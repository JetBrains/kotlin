// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

// TESTCASE NUMBER: 1
fun <T : List<T>> Inv<out T>.case_1() {
    if (this is MutableList<*>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T> & kotlin.collections.MutableList<*> & Inv<out T>")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T> & kotlin.collections.MutableList<*> & Inv<out T>")!>this<!>[0] = <!ARGUMENT_TYPE_MISMATCH!><!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T> & kotlin.collections.MutableList<*> & Inv<out T>")!>this<!>[1]<!>
    }
}
