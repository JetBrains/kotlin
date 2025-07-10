// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidExposingPackagePrivateInInternal
// FILE: Foo.java
class Foo {}

// FILE: test.kt
internal fun <T : <!EXPOSED_TYPE_PARAMETER_BOUND!>Foo<!>> <!EXPOSED_RECEIVER_TYPE!>Foo<!>.<!EXPOSED_FUNCTION_RETURN_TYPE!>bar<!>(<!EXPOSED_PARAMETER_TYPE!>f: Foo<!>): Foo = Foo()

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, javaType */
