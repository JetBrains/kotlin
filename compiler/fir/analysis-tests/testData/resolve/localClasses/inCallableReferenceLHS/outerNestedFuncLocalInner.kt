// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-66344, KT-82122
// LATEST_LV_DIFFERENCE

class Outer<A> {
    class Nested<B> {
        fun <C> func() {
            class Local<D> {
                inner class Inner<E> {
                    val ref1 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class Inner<E, Outer(D), Outer(C), Outer(B)> : Any")!>Inner<E><!>::toString
                    val ref2 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class Inner<E, Outer(D), Outer(C), Outer(B)> : Any")!>Local<D>.Inner<E><!>::toString

                    val ref3 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class Inner<E, Outer(D), Outer(C), Outer(B)> : Any")!>Inner<Int><!>::toString
                    val ref4 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class Inner<E, Outer(D), Outer(C), Outer(B)> : Any")!>Local<String>.Inner<Int><!>::toString
                }

                val ref1 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class Inner<E, Outer(D), Outer(C), Outer(B)> : Any")!>Local<D>.Inner<Int><!>::toString
                val ref2 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class Inner<E, Outer(D), Outer(C), Outer(B)> : Any")!>Local<String>.Inner<Int><!>::toString
            }
            val ref1 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS("3; inner class Inner<E, Outer(D), Outer(C), Outer(B)> : Any")!>Local<C>.Inner<C><!>::toString
        }
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, localProperty,
nestedClass, nullableType, propertyDeclaration, starProjection, typeParameter */
