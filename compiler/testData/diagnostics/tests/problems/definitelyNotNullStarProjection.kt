// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-53840

data class A<T>(
    val a: T,
    val b: T & Any,
)

fun pass(x: Any) {}

fun hello(a: A<*>) {
    pass(a.b)
}

/* GENERATED_FIR_TAGS: classDeclaration, data, dnnType, functionDeclaration, nullableType, primaryConstructor,
propertyDeclaration, starProjection, typeParameter */
