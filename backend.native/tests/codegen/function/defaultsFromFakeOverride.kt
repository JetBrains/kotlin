package codegen.function.defaultsFromFakeOverride

import kotlin.test.*

interface I<T> {
    fun f(x: String = "42"): String
}

open class A<T> {
    open fun f(x: String) = x
}

class B : A<String>(), I<String>

@Test fun runTest() {
    val b = B()
    println(b.f())
}