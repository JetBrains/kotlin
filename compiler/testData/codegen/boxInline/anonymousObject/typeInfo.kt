// FILE: 1.kt

package test

public inline fun <reified T : Any> foo(block: () -> T) = T::class

// FILE: 2.kt

import test.*

fun box(): String {
    foo { object {} }
    return "OK"
}
