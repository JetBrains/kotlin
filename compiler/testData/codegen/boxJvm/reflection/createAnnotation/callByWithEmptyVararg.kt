// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.full.primaryConstructor
import kotlin.test.*

annotation class K1(vararg val value: String)

annotation class K2(val other: Int, vararg val value: Long)

annotation class K3(val value: Array<String>)

fun box(): String {
    val k1 = K1::class.primaryConstructor!!
    assertFalse(k1.parameters.single().isOptional)
    assertEquals(emptyList(), k1.callBy(emptyMap()).value.toList())

    val k2 = K2::class.primaryConstructor!!
    assertEquals(listOf(false, false), k2.parameters.map { it.isOptional })
    assertEquals(emptyList(), k2.callBy(mapOf(k2.parameters.first() to 42)).value.toList())

    val k3 = K3::class.primaryConstructor!!
    assertFalse(k1.parameters.single().isOptional)
    assertFailsWith<IllegalArgumentException> { k3.callBy(emptyMap()) }

    return "OK"
}
