// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test

interface F<T> {
    fun test(p: T) : Int
}

inline fun <T> Array<T>.copyOfRange1(crossinline toIndex: () -> Int) =
        object : F<T> {
            override fun test(p: T): Int {
                return toIndex()
            }
        }

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING

import test.*
import java.util.*

public fun Array<out CharSequence>.slice1() = copyOfRange1 { 1 }

fun box(): String {
    val comparable = arrayOf("123").slice1()
    val method = comparable.javaClass.getMethod("test", Any::class.java)
    val genericParameterTypes = method.genericParameterTypes
    if (genericParameterTypes.size != 1) return "fail 1: ${genericParameterTypes.size}"
    var name = (genericParameterTypes[0] as Class<*>).name
    if (name != "java.lang.CharSequence") return "fail 2: ${name}"

    return "OK"
}
