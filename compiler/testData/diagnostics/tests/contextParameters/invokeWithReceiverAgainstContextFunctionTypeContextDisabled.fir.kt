// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
// ISSUE: KT-73805

fun test(f: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> Boolean.() -> Unit) {
    "".<!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL, UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(1, true)
    with("") {
        with(1) {
            <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>true)<!>
        }
    }
    <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>("", 1, true)
}
