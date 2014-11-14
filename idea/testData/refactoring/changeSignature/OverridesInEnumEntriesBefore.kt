enum class E {
    open fun <caret>foo(n: Int): Int = n

    O
    A {
        override fun foo(n: Int): Int = n + 1

    }
    B {
        override fun foo(n: Int): Int = n + 2

    }
}