// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import kotlin.reflect.KFunction2
import kotlin.test.assertEquals

fun box(): String {
    // References to members of mutable collection classes are not supported by the legacy (K1-based) reflection implementation.
    val systemProperties = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt")
    if (systemProperties.getMethod("getUseK1Implementation").invoke(null) == true ||
        systemProperties.getMethod("getUseK1ImplementationForMembers").invoke(null) == true
    ) return "OK"

    val list = arrayListOf("a")
    val add: KFunction2<MutableList<String>, String, Boolean> = MutableList<String>::add
    assertEquals("fun kotlin.collections.MutableList<E>.add(E): kotlin.Boolean", add.toString())
    assertEquals(true, add.call(list, "b"))
    assertEquals(listOf("a", "b"), list)

    val removeAt = MutableList<String>::removeAt
    assertEquals("fun kotlin.collections.MutableList<E>.removeAt(kotlin.Int): E", removeAt.toString())
    assertEquals("a", removeAt.call(list, 0))
    assertEquals(listOf("b"), list)

    val map = hashMapOf("a" to 1)
    val put = MutableMap<String, Int>::put
    assertEquals("fun kotlin.collections.MutableMap<K, V>.put(K, V): V?", put.toString())
    assertEquals(null, put.call(map, "b", 2))
    assertEquals(mapOf("a" to 1, "b" to 2), map)

    val size = MutableCollection<String>::size
    assertEquals("val kotlin.collections.Collection<E>.size: kotlin.Int", size.toString())
    assertEquals(1, size.call(list))

    val contains = MutableCollection<String>::contains
    assertEquals("fun kotlin.collections.Collection<E>.contains(E): kotlin.Boolean", contains.toString())
    assertEquals(true, contains.call(list, "b"))

    return "OK"
}
