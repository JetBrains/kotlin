package test

inline fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()

class A {
    class X {

    }

    inner class OuterY

    fun outerFoo(n: Int) {}

    val outerBar = 1

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

}