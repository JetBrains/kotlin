// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -UNSUPPORTED_FEATURE, -CONTEXT_RECEIVERS_DEPRECATED

context(_: Any) fun f1() = 0
fun f1() = ""

context(_: Any) fun f2() {}
context(_: String) fun f2() {}

context(_: Any) fun f3() {}
context(_: String, _: Any) fun f3() {}

context(_: String)
fun test() {
    val x: Int = f1()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f3<!>()
}
