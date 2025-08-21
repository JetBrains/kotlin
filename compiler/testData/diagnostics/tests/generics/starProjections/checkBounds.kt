// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A<T : A<T>>
fun <T : A<*>> foo() {}
class B<T : A<*>>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, starProjection, typeConstraint, typeParameter */
