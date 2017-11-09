// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

class A(val value: String) {

    inline fun inlineFun(lambda: () -> String = { value }): String {
        return lambda()
    }
}

// FILE: 2.kt

import test.*

// CHECK_CONTAINS_NO_CALLS: box
fun box(): String {
    return A("OK").inlineFun()
}