package codegen.bridges.test13

import kotlin.test.*

open class A<T> {
    open fun T.foo() {
        println(this.toString())
    }

    fun bar(x: T) {
        x.foo()
    }
}

open class B: A<Int>() {
    override fun Int.foo() {
        println(this)
    }
}

@Test fun runTest() {
    val b = B()
    val a = A<Int>()
    b.bar(42)
    a.bar(42)
}