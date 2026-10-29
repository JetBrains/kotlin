// RUN_PIPELINE_TILL: CODEGEN

@file:Suppress("INLINE_CLASS_DEPRECATED", <!ERROR_SUPPRESSION!>"INLINE_CLASS_DEPRECATED_ERROR"<!>)

inline class IC(val i: Int)

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any, ic: IC)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any?, ic: IC)<!> {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, primaryConstructor, propertyDeclaration */
