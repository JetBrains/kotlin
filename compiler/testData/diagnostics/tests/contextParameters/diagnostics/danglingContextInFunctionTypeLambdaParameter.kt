// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

val lambda = { p: context(Int)<!SYNTAX!><!> -> }
val anonymousFunction = fun(p: context(Int)<!SYNTAX!><!>) {}

val lambdaUnresolved = { p: context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!> -> }
val anonymousFunctionUnresolved = fun(p: context(Int) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>) {}

val nullableLambda = { p: (context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>)? -> }
val nullableAnonymousFunction = fun(p: (context(Int) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>)?) {}

/* GENERATED_FIR_TAGS: anonymousFunction, lambdaLiteral, propertyDeclaration */
