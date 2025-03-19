// TARGET_BACKEND: JVM
// WITH_REFLECT

// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).

package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

annotation class Anno(val klasses: Array<KClass<*>> = arrayOf(String::class, Int::class))

fun box(): String {
    val anno = Anno::class.constructors.single().callBy(emptyMap())
    assertEquals(listOf(String::class, Int::class), anno.klasses.toList())
    assertEquals("@test.Anno(klasses=[class java.lang.String, int])", anno.toString())
    return "OK"
}
