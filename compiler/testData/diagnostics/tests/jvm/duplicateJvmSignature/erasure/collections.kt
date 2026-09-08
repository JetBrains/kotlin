// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(s: List<String>)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(s: MutableList<String>)<!> {}

/* GENERATED_FIR_TAGS: functionDeclaration */
