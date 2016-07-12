package test

inline fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()

class A {
    class X {

    }

    companion object {
        class Y

        fun foo(n: Int) {}

        val bar = 1

        fun Int.extFoo(n: Int) {}

        val Int.extBar: Int get() = 1
    }

    object O {
        class Y

        fun foo(n: Int) {}

        val bar = 1

        fun Int.extFoo(n: Int) {}

        val Int.extBar: Int get() = 1
    }

    class B {
        class <caret>C {
            fun test() {
                X()

                Y()
                foo(bar)
                //1.extFoo(1.extBar) // conflict

                O.Y()
                O.foo(O.bar)

                with(O) {
                    Y()
                    foo(bar)
                    1.extFoo(1.extBar)
                }
            }
        }
    }
}