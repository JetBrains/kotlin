// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

private val prop = "O"

private fun test() = "K"

inline internal fun inlineFun(): String {
    return prop + test()
}

class A () {
    fun call() = inlineFun()
}

// FILE: 2.kt

import test.*
fun box(): String {
    return A().call();
}
