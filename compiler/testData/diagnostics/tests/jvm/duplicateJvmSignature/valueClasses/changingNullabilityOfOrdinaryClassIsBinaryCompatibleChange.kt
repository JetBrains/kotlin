// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -UNUSED_PARAMETER, -INLINE_CLASS_DEPRECATED

inline class IC(val i: Int)

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any, ic: IC)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any?, ic: IC)<!> {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, primaryConstructor, propertyDeclaration */
