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
    val refFoo: Inner.() -> Unit = Inner::foo
    val refBar: Inner.() -> Int = Inner::bar
    val kRefFoo = Inner::foo
    val kRefBar = Inner::bar
    val refIncorrect: Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.Inner<!>::<!UNRESOLVED_REFERENCE!>foo<!>
    val kRefIncorrect = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.Inner<!>::<!UNRESOLVED_REFERENCE!>foo<!>

    class Nested {
        val refFoo: A<*>.Inner.() -> Unit = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::<!UNRESOLVED_REFERENCE!>foo<!>
        val refBar: A<*>.Inner.() -> Int = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::<!UNRESOLVED_REFERENCE!>bar<!>
        val kRefFoo = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::<!UNRESOLVED_REFERENCE!>foo<!>
        val kRefBar = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::<!UNRESOLVED_REFERENCE!>bar<!>
        val refIncorrect: A<*>.Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.Inner<!>::<!UNRESOLVED_REFERENCE!>foo<!>
        val kRefIncorrect = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A.Inner<!>::<!UNRESOLVED_REFERENCE!>foo<!>
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, inner, integerLiteral,
nullableType, objectDeclaration, propertyDeclaration, starProjection, typeParameter, typeWithExtension */
