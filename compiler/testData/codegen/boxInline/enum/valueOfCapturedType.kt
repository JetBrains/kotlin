// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// WITH_RUNTIME
package test

inline fun <reified T : Enum<T>> myValueOf(): String {
    return { enumValueOf<T>("OK") }().name
}

enum class Z {
    OK
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myValueOf<Z>()
}
