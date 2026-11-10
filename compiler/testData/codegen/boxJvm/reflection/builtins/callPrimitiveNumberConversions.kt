// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun KClass<*>.member(name: String): KCallable<*> = members.single { it.name == name }

fun box(): String {
    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        // KotlinReflectionInternalError: Could not compute caller for function: [...] toByte() [...] (member = null)
        return "OK"
    }

    assertEquals(42.toByte(), Int::class.member("toByte").call(42))
    assertEquals(42.toShort(), Int::class.member("toShort").call(42))
    assertEquals(42, Int::class.member("toInt").call(42))
    assertEquals(42L, Int::class.member("toLong").call(42))
    assertEquals(42.0f, Int::class.member("toFloat").call(42))
    assertEquals(42.0, Int::class.member("toDouble").call(42))

    assertEquals(42, Byte::class.member("toInt").call(42.toByte()))
    assertEquals(42, Short::class.member("toInt").call(42.toShort()))
    assertEquals(42, Long::class.member("toInt").call(42L))
    assertEquals(42, Float::class.member("toInt").call(42.5f))
    assertEquals(42, Double::class.member("toInt").call(42.5))

    assertEquals(42.toByte(), Int::toByte.call(42))
    assertEquals(42.toShort(), Int::toShort.call(42))
    assertEquals(42, Int::toInt.call(42))
    assertEquals(42L, Int::toLong.call(42))
    assertEquals(42.0f, Int::toFloat.call(42))
    assertEquals(42.0, Int::toDouble.call(42))

    assertEquals(42, Byte::toInt.call(42.toByte()))
    assertEquals(42, Short::toInt.call(42.toShort()))
    assertEquals(42, Long::toInt.call(42L))
    assertEquals(42, Float::toInt.call(42.5f))
    assertEquals(42, Double::toInt.call(42.5))

    assertEquals(42L, 42::toLong.call())
    assertEquals(42, 42.5::toInt.call())

    assertEquals(Int::class.member("toLong"), Int::toLong)
    assertEquals(Int::class.member("toLong").hashCode(), Int::toLong.hashCode())

    return "OK"
}
