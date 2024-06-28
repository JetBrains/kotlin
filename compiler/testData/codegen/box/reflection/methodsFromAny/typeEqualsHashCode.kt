// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KType
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

fun unit(p: Unit): Unit {}

fun nullable(s: String): String? = s

class A {
    fun <T> typeParam(t: T): T = t
}


fun box(): String {
    fun check(t1: KType, t2: KType) {
        assertEquals(t1, t2)
        assertEquals(t1.hashCode(), t2.hashCode())
    }

    check(::unit.parameters.single().type, ::unit.returnType)

    assertNotEquals(::nullable.parameters.single().type, ::nullable.returnType)

    val typeParam = A::class.members.single { it.name == "typeParam" }
    check(typeParam.parameters.last().type, typeParam.returnType)

    return "OK"
}
