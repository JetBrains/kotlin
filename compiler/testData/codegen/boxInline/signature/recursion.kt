// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test


inline fun stub() {

}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING

import test.*
import java.util.*


class I<A>(val s: A)
class A<T : Any>(val elements: List<I<T>>) {
    val p = elements.sortedBy { it.hashCode() }
}

fun box(): String {

    A(listOf(I("1"), I("2"), I("3"))).p

    return "OK"
}
