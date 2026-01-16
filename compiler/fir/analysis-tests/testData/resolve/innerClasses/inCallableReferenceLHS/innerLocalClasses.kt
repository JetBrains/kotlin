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
        val ref = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<String><!>::foo
        val innermostRef = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<String>.Innermost<Int><!>::bar
        val innermostMisplacedRef = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner.Innermost<Int, String><!>::bar
    }

    Outer<Int>.Inner<String>::foo
    Outer<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!><String, Int><!>.Inner::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_WARNING!>Outer<!>.Inner<String, Int>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, nullableType,
propertyDeclaration, starProjection, typeParameter */
