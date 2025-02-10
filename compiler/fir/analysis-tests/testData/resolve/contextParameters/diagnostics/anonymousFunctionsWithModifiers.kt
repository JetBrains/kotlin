// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
val t = context(a: () -> Unit) inline fun () { runNotInlined (a) }
fun runNotInlined(a: () -> Unit){}

val t2 = context(a: String) tailrec fun() {}
val t3 = context(a: String) operator fun() {}
val t4 = context(a: String) external fun() {}
val t5 = context(a: String) infix fun() {}

fun foo(f: suspend (String) -> Unit) {
}

fun bar() {
    foo(context(s: String) <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun () {})
}