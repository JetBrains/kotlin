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
        x is A<Int>.B2 -> {}
        x is A.C -> {}
    }

    when {
        x is B1 -> {}
        x is B2 -> {}
        x is C -> {} // Not
    }
}
