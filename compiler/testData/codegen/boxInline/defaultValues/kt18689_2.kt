// FILE: 1.kt

package test

class Foo {
    fun foo() = "O"
}

inline fun inlineFn(crossinline fn: () -> String, x: String = "K"): String {
    return fn() + x
}

// FILE: 2.kt

import test.*

private val foo = Foo()

fun box(): String {
    return inlineFn(foo::foo)
}