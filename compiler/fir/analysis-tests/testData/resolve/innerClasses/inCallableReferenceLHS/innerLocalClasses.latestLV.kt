// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344, KT-82122
// LATEST_LV_DIFFERENCE

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
        val innermostMisplacedRef = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_CALLABLE_REFERENCE_LHS_ERROR!>Inner<!>.Innermost<Int, String>::bar
    }

    Outer<Int>.Inner<String>::foo
    Outer<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_CALLABLE_REFERENCE_LHS_ERROR!><String, Int><!>.Inner::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_CALLABLE_REFERENCE_LHS_ERROR!>Outer<!>.Inner<String, Int>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, nullableType,
propertyDeclaration, starProjection, typeParameter */
