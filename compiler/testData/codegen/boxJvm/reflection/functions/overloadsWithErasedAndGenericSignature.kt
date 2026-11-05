// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals

interface I {
    fun <T> f(x: T): CharSequence = "a"
    fun f(x: Any?): String = "b"

    fun <T> g(x: Array<T>): CharSequence = "a"
    fun g(x: Array<Any?>): String = "b"

    fun <T> h(x: List<T>): CharSequence = "a"
    fun h(x: List<Any?>): String = "b"
}

class C : I

class D {
    fun <T> f(x: T): CharSequence = "a"
    fun f(x: Any?): String = "b"

    fun <T> g(x: Array<T>): CharSequence = "a"
    fun g(x: Array<Any?>): String = "b"

    fun <T> h(x: List<T>): CharSequence = "a"
    fun h(x: List<Any?>): String = "b"
}

fun box(): String {
    for (k in listOf(I::class, C::class, D::class)) {
        for (name in listOf("f", "g", "h")) {
            assertEquals(2, k.members.filter { it.name == name }.size, "Members '$name' of $k")
            for (method in k.java.methods.filter { it.name == name }) {
                val f = method.kotlinFunction ?: return "Fail: no kotlinFunction for $method"
                assertEquals(method.returnType, f.javaMethod?.returnType)
            }
        }
    }
    return "OK"
}
