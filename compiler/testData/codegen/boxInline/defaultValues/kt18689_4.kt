// FILE: 1.kt

package test

class Foo {
    fun foo() = "OK"
}

inline fun inlineFn(crossinline fn: () -> String, x: Long = 1L): String {
    return fn()
}

// FILE: 2.kt

import test.*

private val foo = Foo()

fun box(): String {
    return inlineFn(foo::foo)
}