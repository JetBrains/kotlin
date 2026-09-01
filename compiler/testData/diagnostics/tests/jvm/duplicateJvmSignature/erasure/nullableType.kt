// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: String)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: String?)<!> {}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType */
