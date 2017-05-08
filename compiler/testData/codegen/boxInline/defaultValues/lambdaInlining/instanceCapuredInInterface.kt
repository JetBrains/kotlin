// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

//problem in test framework
inline fun inlineFunStub(){}

interface A {
    val value: String

    fun test() = inlineFun()

    @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
    private inline fun inlineFun(lambda: () -> String = { value }): String {
        return lambda()
    }
}

// FILE: 2.kt

import test.*

class B : A {
    override val value: String = "OK"
}

fun box(): String {
    return B().test()
}