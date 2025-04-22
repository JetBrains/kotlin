// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES
// ISSUE: KT-76739

interface Foo<T>

fun <T> foo(s: String) {}
fun test() {
    foo<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("1; interface Foo<T> : Any")!>Foo<!>>(<!ARGUMENT_TYPE_MISMATCH("String; String")!>""<!>)
}
