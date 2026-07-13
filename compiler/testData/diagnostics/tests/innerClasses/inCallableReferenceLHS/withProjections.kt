// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344

class Outer<A> {
    inner class Inner<B> {
        fun foo(b: B) {}
        fun bar(): B = null!!
    }

    val refBarIn: Outer<A>.Inner<in CharSequence>.() -> CharSequence <!INITIALIZER_TYPE_MISMATCH!>=<!> Inner<in CharSequence>::bar
    val refFooIn: Outer<A>.Inner<in CharSequence>.(CharSequence) -> Unit = Inner<in CharSequence>::foo
    val refBarOut: Outer<A>.Inner<out CharSequence>.() -> CharSequence = Inner<out CharSequence>::bar
    val refFooOut: Outer<A>.Inner<out CharSequence>.(CharSequence) -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> Inner<out CharSequence>::foo
    val refBarStar = Inner<*>::foo
    val refFooStar = Inner<*>::bar
}

/* GENERATED_FIR_TAGS: callableReference, checkNotNullCall, classDeclaration, functionDeclaration, functionalType,
inProjection, inner, nullableType, out, outProjection, propertyDeclaration, starProjection, typeParameter,
typeWithExtension */
