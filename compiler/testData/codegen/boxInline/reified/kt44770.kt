// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// IGNORE_BACKEND: ANDROID
// SKIP_JDK6
// FILE: 1.kt

package test
import java.lang.reflect.*

class OK

interface S<A>

inline fun <reified T : Any> test(): String {
    return (object : S<T> {

    }::class.java.genericInterfaces[0] as ParameterizedType).actualTypeArguments[0].getTypeName()
}

// FILE: 2.kt
import test.*

fun box():String {
    val test = test<OK>()
    return if (test == OK::class.qualifiedName) "OK" else "fail: $test"
}
