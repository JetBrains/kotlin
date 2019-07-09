// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test

interface Call<T> {
    fun call(): T
}

public inline fun <reified T: Any> Any.inlineMeIfYouCan() : () -> Call<T> = {
    object : Call<T> {
        override fun call() = T::class.java.newInstance()
    }
}

// FILE: 2.kt

import test.*

public class A()

fun box(): String {
    val s = "yo".inlineMeIfYouCan<A>()().call()
    if (s !is A) return "fail"

    return "OK"
}
