// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344

abstract class A<X> {
    inner class Inner {
        fun foo() {}
        val bar: Int = 42
    }
    fun baz() {
    }
}

object B : A<String>() {
    val refFoo: Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::foo
    val refBar: Inner.() -> Int = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::bar
    val kRefFoo = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::foo
    val kRefBar = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::bar
    val refIncorrect: Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.Inner<!>::foo
    val kRefIncorrect = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.Inner<!>::foo

    class Nested {
        val refFoo: A<*>.Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::foo
        val refBar: A<*>.Inner.() -> Int = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::bar
        val kRefFoo = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::foo
        val kRefBar = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::bar
        val refIncorrect: A<*>.Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.Inner<!>::foo
        val kRefIncorrect = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.Inner<!>::foo
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, inner, integerLiteral,
nullableType, objectDeclaration, propertyDeclaration, starProjection, typeParameter, typeWithExtension */
