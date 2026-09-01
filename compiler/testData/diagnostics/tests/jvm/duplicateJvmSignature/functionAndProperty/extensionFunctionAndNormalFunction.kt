// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: Any)<!> {}
    <!CONFLICTING_JVM_DECLARATIONS!>fun Any.foo()<!> {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
