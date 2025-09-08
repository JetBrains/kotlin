// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke() {}

class SomeClass

fun test(identifier: SomeClass, fn: String.() -> Unit) {
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>identifier<!>()
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>identifier<!>(123)
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>identifier<!>(1, 2)
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fn<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral,
typeWithExtension */
