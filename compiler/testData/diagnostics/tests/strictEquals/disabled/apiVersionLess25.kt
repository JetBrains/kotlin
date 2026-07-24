// API_VERSION: 2.4
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -StrictEquals

abstract class A {
    override fun equals(<!UNSUPPORTED_FEATURE!>@<!API_NOT_AVAILABLE!>EqualityBound<!>(A::class)<!> other: Any?): Boolean = true

    open class B {
        override fun equals(<!UNSUPPORTED_FEATURE!>@<!API_NOT_AVAILABLE!>EqualityBound<!>(B::class)<!> other: Any?): Boolean = true
    }

    inner class C : B() {
        override operator fun equals(<!UNSUPPORTED_FEATURE!>@<!API_NOT_AVAILABLE!>EqualityBound<!>(B::class)<!> other: Any?): Boolean = true
    }
}

fun local() {
    val d = object : A() {
        override fun equals(<!UNSUPPORTED_FEATURE!>@<!API_NOT_AVAILABLE!>EqualityBound<!>(A::class)<!> other: Any?): Boolean = true
    }

    class E {
        override operator fun equals(<!UNSUPPORTED_FEATURE!>@<!API_NOT_AVAILABLE!>EqualityBound<!>(E::class)<!> other: Any?): Boolean = true
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, classReference, functionDeclaration, inner,
localClass, localProperty, nestedClass, nullableType, operator, propertyDeclaration */
