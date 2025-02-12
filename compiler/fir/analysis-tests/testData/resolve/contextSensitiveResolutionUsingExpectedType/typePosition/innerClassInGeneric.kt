// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class A<T> {
    inner class B1 : A<String>() {
        fun foo(): T = TODO()
    }

    inner class B2 : A<String>() {
        fun foo(): T = TODO()
    }


    class C : A<Int>()
}

fun foo(x: A<Int>) {
    when {
        x is A<*>.B1 -> {}
        x is <!CANNOT_CHECK_FOR_ERASED!>A<Int>.B2<!> -> {}
        x is A.C -> {}
    }

    when {
        x is <!UNRESOLVED_REFERENCE!>B1<!> -> {}
        x is <!UNRESOLVED_REFERENCE!>B2<!> -> {}
        x is C -> {} // Not
    }
}
