// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Foo> foo(x: T): T<!> {null!!}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: Foo): Foo<!> {null!!}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, interfaceDeclaration, typeConstraint, typeParameter */
