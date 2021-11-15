// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// FILE: 1.kt

package test
inline fun inlineFun(vararg constraints: String, receiver: String = "K", init: String.() -> String): String {
    return (constraints.joinToString() + receiver).init()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun("O") {
        this
    }
}
