// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals

interface I {
    fun f(x: Collection<CharSequence>): CharSequence = "a"
    fun f(x: Collection<String>): String = "b"
}

class C : I

class D {
    fun f(x: Collection<CharSequence>): CharSequence = "a"
    fun f(x: Collection<String>): String = "b"
}

fun box(): String {
    for (k in listOf(I::class, C::class, D::class)) {
        assertEquals(2, k.members.filter { it.name == "f" }.size, "Members of $k")
        for (method in k.java.methods.filter { it.name == "f" }) {
            val f = method.kotlinFunction ?: return "Fail: no kotlinFunction for $method"
            assertEquals(method.returnType, f.javaMethod?.returnType)
        }
    }
    return "OK"
}
