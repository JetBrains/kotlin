package test

inline fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()

class A() {
    fun X.bar() {}
}

class X {
    fun Int.foo() {}

    inner class Y {
        fun test() {
            1.foo()
            with(1) { foo() }
            with(A()) { bar() }
        }
    }
}