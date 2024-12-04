// WITH_REFLECT
// FULL_JDK
// NO_CHECK_LAMBDA_INLINING
// LAMBDAS: CLASS
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

open class Test {

    inline fun <Y> test(z: () -> () -> Y) = z()

    fun <T> callInline(p: T)  = test<T> {
        {
            p
        }
    }
}

// FILE: 2.kt


import test.*
import java.util.*


fun box(): String {
    val result = Test().callInline("test")

    val method = result.javaClass.getMethod("invoke")
    val genericReturnType = method.genericReturnType
    if (genericReturnType.toString() != "T") return "fail 1: $genericReturnType"

    val method2 = Test::class.java.getMethod("callInline", Any::class.java)
    val genericParameterType = method2.genericParameterTypes.firstOrNull()

    if (genericParameterType != genericReturnType) return "fail 2: $genericParameterType != $genericReturnType"

    return "OK"
}
