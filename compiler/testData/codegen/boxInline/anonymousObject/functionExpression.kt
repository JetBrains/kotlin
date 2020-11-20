// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

interface Foo {
    fun compute(): Int
}

inline fun foo(x: Int, block: (Int) -> Foo) = block(x)

// FILE: 2.kt

import test.*

fun bar(): Int {
    return foo(21) { x ->
        val o = object : Foo {
            override fun compute(): Int {
                return call { x * 2 }
            }

            private fun call(f: () -> Int) = f()
        }
        o
    }.compute()
}

fun box() = if (bar() == 42) "OK" else "fail"
