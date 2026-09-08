// TARGET_BACKEND: JVM
// WITH_REFLECT
package test

import kotlin.reflect.*
import kotlin.test.assertEquals

open class A<T, U> {
    var member: U
        get() = null!!
        set(value) {}

    var T.memberExtension: U
        get() = null!!
        set(value) {}
}

class Z : A<String, Int>()

private fun KCallable<*>.parameterTypes(): List<String> =
    parameters.map { it.type.toString() }

fun box(): String {
    val member = Z::class.members.single { it.name == "member" } as KMutableProperty1<*, *>
    assertEquals("var test.Z.member: kotlin.Int", member.toString())
    assertEquals(listOf("test.Z"), member.parameterTypes())
    assertEquals(listOf("test.Z"), member.getter.parameterTypes())
    assertEquals(listOf("test.Z", "kotlin.Int"), member.setter.parameterTypes())

    val memberExtension = Z::class.members.single { it.name == "memberExtension" } as KMutableProperty2<*, *, *>
    assertEquals("var test.Z.(kotlin.String.)memberExtension: kotlin.Int", memberExtension.toString())
    assertEquals(listOf("test.Z", "kotlin.String"), memberExtension.parameterTypes())
    assertEquals(listOf("test.Z", "kotlin.String"), memberExtension.getter.parameterTypes())
    assertEquals(listOf("test.Z", "kotlin.String", "kotlin.Int"), memberExtension.setter.parameterTypes())
    return "OK"
}
