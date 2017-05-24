// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
// CHECK_CONTAINS_NO_CALLS: test
package test

//problem in test framework
inline fun inlineFunStub(){}

interface A {
    val value: String

    fun test() = inlineFun()

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