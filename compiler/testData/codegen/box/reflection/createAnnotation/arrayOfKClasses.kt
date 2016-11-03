// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.test.assertEquals

annotation class Anno(val klasses: Array<KClass<*>> = arrayOf(String::class, Int::class))

fun box(): String {
    val anno = Anno::class.constructors.single().callBy(emptyMap())
    assertEquals(listOf(String::class, Int::class), (anno.klasses as Array<KClass<*>>).toList() /* TODO: KT-9453 */)
    assertEquals("@Anno(klasses=[class java.lang.String, int])", anno.toString())
    return "OK"
}
