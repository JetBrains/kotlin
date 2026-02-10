// ISSUE: KT-83524
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun test(foo: context(Int) (String) -> Unit) {
    foo(b = "")
}