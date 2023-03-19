// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

inline class S(val value: String)

fun S.foo(x: Int, s: S): S = this

inline class T(val s: S) {
    fun bar(u: S): T = this
}

fun box(): String {
    assertEquals(listOf(String::class.java, Int::class.java, String::class.java), S::foo.parameters.map { it.type.javaType })
    assertEquals(String::class.java, S::foo.returnType.javaType)

    assertEquals(listOf(String::class.java, String::class.java), T::bar.parameters.map { it.type.javaType })
    assertEquals(String::class.java, T::bar.returnType.javaType)

    return "OK"
}
