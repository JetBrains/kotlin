// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344

class Outer<A> {
    inner class Inner<B> {
        fun foo(b: B) {}
        fun bar(): B = null!!
    }

    val refBarIn: Outer<A>.Inner<in CharSequence>.() -> CharSequence <!INITIALIZER_TYPE_MISMATCH!>=<!> Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><in CharSequence><!>::bar
    val refFooIn: Outer<A>.Inner<in CharSequence>.(CharSequence) -> Unit = Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><in CharSequence><!>::foo
    val refBarOut: Outer<A>.Inner<out CharSequence>.() -> CharSequence = Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><out CharSequence><!>::bar
    val refFooOut: Outer<A>.Inner<out CharSequence>.(CharSequence) -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><out CharSequence><!>::foo
    val refBarStar = Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    val refFooStar = Inner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::bar
}

/* GENERATED_FIR_TAGS: callableReference, checkNotNullCall, classDeclaration, functionDeclaration, functionalType,
inProjection, inner, nullableType, out, outProjection, propertyDeclaration, starProjection, typeParameter,
typeWithExtension */
