// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun foo(f: context(String) () -> String) {}

fun test() {
    // Should become green when context receivers are removed and we parse `c` as parameter without name instead of context receiver.
    foo(<!ARGUMENT_TYPE_MISMATCH!>context(<!CONTEXT_PARAMETER_WITHOUT_NAME, UNRESOLVED_REFERENCE!>c<!>) fun() { return <!UNRESOLVED_REFERENCE!>c<!> }<!>)
}