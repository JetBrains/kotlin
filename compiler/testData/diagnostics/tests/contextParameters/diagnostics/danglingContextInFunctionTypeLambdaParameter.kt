// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

val lambda = { p: context(Int)<!SYNTAX!><!> -> }
val anonymousFunction = fun(p: context(Int)<!SYNTAX!><!>) {}

val lambdaUnresolved = { p: context(Unresolved)<!SYNTAX!><!> -> }
val anonymousFunctionUnresolved = fun(p: context(Int) <!UNRESOLVED_REFERENCE!>Unresolved<!><!SYNTAX!><!>) {}

/* GENERATED_FIR_TAGS: anonymousFunction, lambdaLiteral, propertyDeclaration */
