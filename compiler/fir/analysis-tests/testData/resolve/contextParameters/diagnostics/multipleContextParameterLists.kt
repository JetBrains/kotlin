// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

context(s: String) context(i: Int)
fun foo() {}

context(s: String) context(i: Int)
val bar: String get() = ""