// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: 1.kt
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestKt")
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
