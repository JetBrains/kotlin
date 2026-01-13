// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344
// LATEST_LV_DIFFERENCE

abstract class A<X> {
    inner class Inner {
        fun foo() {}
        val bar: Int = 42
    }
    fun baz() {
    }
}

object B : A<String>() {
    val refFoo: Inner.() -> Unit = Inner::foo
    val refBar: Inner.() -> Int = Inner::bar
    val kRefFoo = Inner::foo
    val kRefBar = Inner::bar
    val refIncorrect: Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_CALLABLE_REFERENCE_LHS_ERROR!>A<!>.Inner::foo
    val kRefIncorrect = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_CALLABLE_REFERENCE_LHS_ERROR!>A<!>.Inner::foo

    class Nested {
        val refFoo: A<*>.Inner.() -> Unit = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::foo
        val refBar: A<*>.Inner.() -> Int = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::bar
        val kRefFoo = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::foo
        val kRefBar = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::bar
        val refIncorrect: A<*>.Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_CALLABLE_REFERENCE_LHS_ERROR!>A<!>.Inner::foo
        val kRefIncorrect = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_CALLABLE_REFERENCE_LHS_ERROR!>A<!>.Inner::foo
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, inner, integerLiteral,
nullableType, objectDeclaration, propertyDeclaration, starProjection, typeParameter, typeWithExtension */
