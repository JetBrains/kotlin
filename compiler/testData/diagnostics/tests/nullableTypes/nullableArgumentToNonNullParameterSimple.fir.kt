// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: String) {}
fun foo(x: Int) {}
fun foo(x: Int, y: String) {}

fun bar(nullX: Int?, nullY: String?, notNullY: String) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>nullX<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>nullX<!>, notNullY)
    foo(<!ARGUMENT_TYPE_MISMATCH!>nullX<!>, <!ARGUMENT_TYPE_MISMATCH!>nullY<!>)
    <!NONE_APPLICABLE!>foo<!>()
}
