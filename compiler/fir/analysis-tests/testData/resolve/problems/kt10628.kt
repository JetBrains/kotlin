// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-10628

// KT-10628: Wrong type mismatch with star projection of inner class inside use-site projected type
class Outer<T> {
    inner class Inner<E : T> {
        fun foo(): E = null!!
    }
}

fun bar(x: Outer<out CharSequence>.Inner<*>) {
    val cs: CharSequence = x.foo() // Type mismatch. Required: CharSequence, Found: Any?
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, functionDeclaration, inner, localProperty,
nullableType, outProjection, propertyDeclaration, starProjection, typeConstraint, typeParameter */
