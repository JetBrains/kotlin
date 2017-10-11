package codegen.bridges.test9

import kotlin.test.*

// abstract class vtable call
abstract class A {
    abstract fun foo(): String
}

abstract class B : A()

class Z : B() {
    override fun foo() = "Z"
}


fun box(): String {
    val z = Z()
    val b: B = z
    val a: A = z
    return when {
        z.foo() != "Z" -> "Fail #1"
        b.foo() != "Z" -> "Fail #2"
        a.foo() != "Z" -> "Fail #3"
        else -> "OK"
    }
}

@Test fun runTest() {
    println(box())
}