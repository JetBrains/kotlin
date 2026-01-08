// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-66344

fun <T> withGenericArg() {
    class Outer<A> {
        inner class Inner<B> {
            inner class Innermost<C> {
                fun bar() {
                }
            }
            fun foo() {}
        }
        val ref = Inner<String>::foo
        val innermostRef = Inner<String>.Innermost<Int>::bar

        // Should be red once KT-82122 is fixed
        val innermostMisplacedRef = Inner.Innermost<Int, String>::bar
    }

    Outer<Int>.Inner<String>::foo

    // Should be red once KT-82122 is fixed
    Outer<String, Int>.Inner::foo
    Outer.Inner<String, Int>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, nullableType,
propertyDeclaration, starProjection, typeParameter */
