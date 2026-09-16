// RUN_PIPELINE_TILL: CODEGEN

interface A<out K> {
    fun foo(x: @UnsafeVariance K): Unit
}

fun test(a: A<*>) {
    a.foo(null)
    a.foo(Any())
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, out, starProjection, typeParameter */
