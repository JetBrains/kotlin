// WITH_REFLECT
// NO_CHECK_LAMBDA_INLINING
// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63855

// FILE: 1.kt
package test

interface F<T> {
    fun test(p: T) : Int
}

inline fun <T: Any?> Array<T>.copyOfRange1(crossinline toIndex: () -> Int) =
        object : F<T> {
            override fun test(p: T): Int {
                return toIndex()
            }
        }

// FILE: 2.kt


import test.*
import java.util.*

public fun Array<in String>.slice1() = copyOfRange1 { 1 }

fun box(): String {
    val comparable = arrayOf("123").slice1()
    val method = comparable.javaClass.getMethod("test", Any::class.java)
    val genericParameterTypes = method.genericParameterTypes
    if (genericParameterTypes.size != 1) return "fail 1: ${genericParameterTypes.size}"
    var name = (genericParameterTypes[0] as Class<*>).name
    if (name != "java.lang.Object") return "fail 2: ${name}"

    return "OK"
}
