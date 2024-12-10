// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
// ISSUE: KT-73805

fun test(f: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> Boolean.() -> Unit) {
    "".f(1, true)
}
