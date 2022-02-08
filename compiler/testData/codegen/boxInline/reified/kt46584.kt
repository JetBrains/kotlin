// FILE: 1.kt
package test

inline fun foo(crossinline x: () -> String) = { x() }()

inline fun <reified T> bar() = foo { { T::class.simpleName!! }() }

// FILE: 2.kt
import test.*

class OK

fun box() = bar<OK>()
