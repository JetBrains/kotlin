package test

interface A {
    interface B {
        /**
         * [<expr>A.foo</expr>]
         */
        fun test(): Unit
    }

    fun foo(): Int
}