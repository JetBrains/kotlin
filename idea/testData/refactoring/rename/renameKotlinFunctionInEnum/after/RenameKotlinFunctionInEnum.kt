package test

enum class E {
    open fun bar(n: Int): Int = n

    O
    A {
        override fun bar(n: Int): Int = n + 1

    }
    B {
        override fun bar(n: Int): Int = n + 2

    }
}