// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
val t = context(a: () -> Unit) <!NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION!>inline<!> fun () { runNotInlined (a) }
fun runNotInlined(a: () -> Unit){}

val t2 = context(a: String) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun() {}
val t3 = context(a: String) <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun() {}
val t4 = context(a: String) <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external<!> fun() {}
val t5 = context(a: String) <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun() {}

fun foo(f: suspend (String) -> Unit) {
}

fun bar() {
    foo(context(s: String) <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun () {})
}