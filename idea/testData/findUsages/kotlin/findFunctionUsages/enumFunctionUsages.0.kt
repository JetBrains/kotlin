// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
enum class E {
    {
        foo(1)
    }

    open fun <caret>foo(n: Int): Int = n

    O
    A {
        {
            foo(1)
        }

        override fun foo(n: Int): Int = n + 1
    }
    B {
        {
            foo(1)
        }

        override fun foo(n: Int): Int = n + 2
    }
}
