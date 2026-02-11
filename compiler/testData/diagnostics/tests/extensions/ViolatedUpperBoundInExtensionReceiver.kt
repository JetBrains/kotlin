// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers, -ContextParameters
interface I<T: Number>

fun I<<!UPPER_BOUND_VIOLATED!>String<!>>.foo() {}

context(I<<!UPPER_BOUND_VIOLATED!>String<!>>)
fun bar() {}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, typeConstraint, typeParameter */
