// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
enum class E {
    init {
        foo(1)
    }

    open fun <caret>foo(n: Int): Int = n

    O
    A {
        init {
            foo(1)
        }

        override fun foo(n: Int): Int = n + 1
    }
    B {
        init {
            foo(1)
        }

        override fun foo(n: Int): Int = n + 2
    }
}
