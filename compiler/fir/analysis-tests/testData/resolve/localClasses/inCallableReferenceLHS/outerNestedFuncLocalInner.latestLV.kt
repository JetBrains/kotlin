// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-66344, KT-82122
// LATEST_LV_DIFFERENCE

class Outer<A> {
    class Nested<B> {
        fun <C> func() {
            class Local<D> {
                inner class Inner<E> {
                    val ref1 = Inner<E>::toString
                    val ref2 = Local<D>.Inner<E>::toString

                    val ref3 = Inner<Int>::toString
                    val ref4 = Local<String>.Inner<Int>::toString
                }

                val ref1 = Local<D>.Inner<Int>::toString
                val ref2 = Local<String>.Inner<Int>::toString
            }
            val ref1 = Local<C>.Inner<C>::toString
        }
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, localProperty,
nestedClass, nullableType, propertyDeclaration, starProjection, typeParameter */
