// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// WITH_STDLIB
// LANGUAGE: +ContextReceivers
// ISSUE: KT-64607

val Number.a get() = ""
fun Number.b() = ""
context(Number) val c get() = ""
context(Number) fun d() = ""

fun test() {
    2U.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>a<!>
    2U.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>b<!>()
    with (2U) {
        <!NO_CONTEXT_ARGUMENT!>c<!>
        <!NO_CONTEXT_ARGUMENT!>d<!>()
    }
}
