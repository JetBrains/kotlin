// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
interface I<T: Number>

fun I<<!UPPER_BOUND_VIOLATED!>String<!>>.foo() {}

context(_: I<<!UPPER_BOUND_VIOLATED!>String<!>>)
fun bar() {}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, typeConstraint, typeParameter */
