// IGNORE_BACKEND_FIR: JVM_IR
// TODO: investigate should it be ran for JS or not
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

object Host {
    fun foo(i: Int, s: String) {}
}

fun box(): String {
    val fooParams = Host::foo.parameters

    assertEquals(2, fooParams.size)

    assertEquals("i", fooParams[0].name)
    assertEquals(Int::class.java, fooParams[0].type.javaType)

    assertEquals("s", fooParams[1].name)
    assertEquals(String::class.java, fooParams[1].type.javaType)

    return "OK"
}