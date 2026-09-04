// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> foo(x: T): T<!> {null!!}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: Any): Any<!> {null!!}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, nullableType, typeParameter */
