// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

public inline fun <T, R> T.myLet(f: (T) -> R): R = f(this)

// FILE: 2.kt

import test.*

interface foo {
    fun bar(): String
}

fun box(): String {
    val baz = "OK".myLet {
        object : foo {
            override fun bar() = it
        }
    }
    return baz.bar()
}
