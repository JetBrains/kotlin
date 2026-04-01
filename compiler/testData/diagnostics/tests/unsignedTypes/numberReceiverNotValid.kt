// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +ContextParameters
// ISSUE: KT-64607

val Number.a get() = ""
fun Number.b() = ""
context(_: Number) val c get() = ""
context(_: Number) fun d() = ""

fun test() {
    2U.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>a<!>
    2U.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>b<!>()
    with (2U) {
        <!NO_CONTEXT_ARGUMENT!>c<!>
        <!NO_CONTEXT_ARGUMENT!>d<!>()
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, getter,
lambdaLiteral, propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral,
unsignedLiteral */
