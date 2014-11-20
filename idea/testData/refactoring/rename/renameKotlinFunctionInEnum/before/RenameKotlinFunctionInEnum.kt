package test

enum class E {
    open fun foo(n: Int): Int = n

    O
    A {
        override fun foo(n: Int): Int = n + 1

    }
    B {
        override fun foo(n: Int): Int = n + 2

    }
}