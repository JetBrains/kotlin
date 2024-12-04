// FILE: 1.kt
package test

inline fun foo(crossinline x: () -> String) = { x() }.let { it() }

inline fun <reified T> bar() = foo { { T::class.simpleName!! }.let { it() } }

// FILE: 2.kt
import test.*

class OK

fun box() = bar<OK>()
