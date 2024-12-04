// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

inline fun <T> myRun(block: () -> T) = block()

// FILE: 2.kt

import test.*

interface IFoo {
    fun foo(): String
}

class A(val x: String, f: () -> IFoo = {
    val y = "K"
    myRun {
        val o = object: IFoo {
            override fun foo() = x + y
        }
        o
    }
}) {
    val foo: IFoo = f()
}

fun box(): String {
    return A("O").foo.foo()
}
