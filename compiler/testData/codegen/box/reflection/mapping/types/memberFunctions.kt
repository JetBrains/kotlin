// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class A {
    fun foo(t: Long?): Long = t!!
}

object O {
    @JvmStatic
    fun bar(a: A): String = ""
}

fun box(): String {
    val foo = A::foo
    assertEquals(listOf(A::class.java, java.lang.Long::class.java), foo.parameters.map { it.type.javaType })
    assertEquals(java.lang.Long.TYPE, foo.returnType.javaType)

    val bar = O::class.members.single { it.name == "bar" }
    assertEquals(listOf(O::class.java, A::class.java), bar.parameters.map { it.type.javaType })
    assertEquals(String::class.java, bar.returnType.javaType)

    return "OK"
}
