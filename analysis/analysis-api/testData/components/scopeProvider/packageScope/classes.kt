// FILE: I.kt
package test.api

interface I

internal interface InternalI

private interface PrivateI

// FILE: A.kt
package test

import test.api.*

abstract class A : I {
    fun perform() { }

    val x: Int = 0

    companion object {
        val y: Int = 0
    }
}

internal abstract class InternalA : InternalI

private abstract class PrivateA : I

// FILE: B.kt
package test

class B : A() {
    fun foo(): Int = 5

    val bar: String = ""

    class InnerClass

    object InnerObject

    companion object {
        val baz: String = ""
    }
}

internal class InternalB : InternalA()

private class PrivateB : A()

// FILE: main.kt
// package: test
