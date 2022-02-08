// WITH_REFLECT
// FULL_JDK
// NO_CHECK_LAMBDA_INLINING
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

class B<T>

interface A {
    fun <T> aTest(p: T): B<T>
}

open class Test {

    inline fun <T> test(crossinline z: () -> Int) = object : A {
        override fun <T> aTest(p: T): B<T> {
            z()
            return B<T>()
        }
    }

    fun callInline() =  test<String> { 1 }
}

// FILE: 2.kt


import test.*
import java.util.*


fun box(): String {
    val result = Test().callInline()
    val method = result.javaClass.getMethod("aTest", Any::class.java)
    val genericReturnType = method.genericReturnType
    if (genericReturnType.toString() != "test.B<T>") return "fail 1: ${genericReturnType}"

    val genericParameterTypes = method.genericParameterTypes
    if (genericParameterTypes.size != 1) return "fail 2: ${genericParameterTypes.size}"
    if (genericParameterTypes[0].toString() != "T") return "fail 3: ${genericParameterTypes[0]}"

    return "OK"
}
