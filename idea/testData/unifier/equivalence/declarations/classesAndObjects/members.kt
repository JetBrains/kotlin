// DISABLE-ERRORS
fun foo() {
    <selection>class A {
        fun a(x: Int): Int = b(x) + 1
        fun b(y: Int): Int = a(y) - 1

        init {
            println(1)
        }

        class X
        inner class Y
        interface T
        object O

        val p = 1
        val q = p + 1

        init {
            println(2)
        }

        companion object {
            fun f() = 1
            val g = f() - 1
        }
    }</selection>

    class B {
        init {
            println(1)
        }

        class X

        fun b(p: kotlin.Int): kotlin.Int = a(p) - 1

        inner class Y

        val p = 1

        companion object {
            fun f() = 1
            val g = f() - 1
        }

        init {
            println(2)
        }

        object O

        val q = p + 1

        fun a(p: kotlin.Int): kotlin.Int = b(p) + 1

        interface T
    }

    class C {
        fun aa(x: Int): Int = bb(x) + 1
        fun bb(y: Int): Int = aa(y) - 1

        init {
            println(1)
        }

        class X
        inner class Y
        interface T
        object O

        val p = 1
        val q = p + 1

        init {
            println(2)
        }

        companion object {
            fun f() = 1
            val g = f() - 1
        }
    }

    class D {
        fun a(x: Int): Int = b(x) + 1
        fun b(y: Int): Int = a(y) - 1

        init {
            println(1)
        }

        class X
        inner class Y
        interface T
        object O

        val q = p + 1
        val p = 1

        init {
            println(2)
        }

        companion object {
            fun f() = 1
            val g = f() - 1
        }
    }

    class E {
        fun a(x: Int): Int = b(x) + 1
        fun b(y: Int): Int = a(y) - 1

        init {
            println(1)
        }

        class XX
        inner class Y
        interface T
        object O

        val p = 1
        val q = p + 1

        init {
            println(2)
        }

        companion object {
            fun f() = 1
            val g = f() - 1
        }
    }
}