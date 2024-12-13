// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
// ISSUE: KT-73805

fun test(f: <!UNSUPPORTED_FEATURE!><!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String, Int)<!> Boolean.() -> Unit) {
    "".f(1, true)
}
