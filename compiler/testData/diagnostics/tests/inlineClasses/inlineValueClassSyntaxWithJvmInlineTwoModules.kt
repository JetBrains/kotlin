// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// MODULE: lib
<!REDUNDANT_JVM_INLINE!>@JvmInline<!>
inline value class New(val x: Int)

<!REDUNDANT_JVM_INLINE!>@JvmInline<!>
value inline class NewReversed(val x: Int)

// MODULE: main(lib)
fun f(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: New) {}
fun g(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> xs: NewReversed) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, value, vararg */
