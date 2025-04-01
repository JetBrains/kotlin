// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
// ISSUE: KT-73805

fun test(f: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> Boolean.() -> Unit) {
    "".<!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(1, true)
    with("") {
        with(1) {
            f(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>true)<!>
        }
    }
    f("", 1, true)
}
