enum class <caret>E {
    open fun foo(n: Int): Int = n

    O
    A {
        override fun foo(n: Int): Int = n + 1

    }
    B {
        override fun foo(n: Int): Int = n + 2

    }
}

// REF: (E).A
// REF: (E).B