// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@JvmInline
value class S(val value: Int) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

val default = S(1000)

class C {
    fun member(x: S, y: Int, z: S?): S = x + S(y) + z!!

    fun memberDefault1_1(x: S = default): S = x
    fun memberDefault1_2(x: S? = default): S = x!!

    fun memberDefault32_1(
        arg00: Long = 0L, arg01: Long = 1L, arg02: Long = 2L, arg03: Long = 3L, arg04: Long = 4L,
        arg05: Long = 5L, arg06: Long = 6L, arg07: Long = 7L, arg08: Long = 8L, arg09: Long = 9L,
        arg10: Long = 10L, arg11: Long = 11L, arg12: Long = 12L, arg13: Long = 13L, arg14: Long = 14L,
        arg15: Long = 15L, arg16: Long = 16L, arg17: Long = 17L, arg18: Long = 18L, arg19: Long = 19L,
        arg20: Long = 20L, arg21: Long = 21L, arg22: Long = 22L, arg23: Long = 23L, arg24: Long = 24L,
        arg25: Long = 25L, arg26: Long = 26L, arg27: Long = 27L, arg28: Long = 28L, arg29: Long = 29L,
        arg30: Long = 30L, x: S = default
    ): S = listOf(
        arg00, arg01, arg02, arg03, arg04, arg05, arg06, arg07, arg08, arg09,
        arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19,
        arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29,
        arg30
    ).sum().let { S(it.toInt()) } + x
    fun memberDefault32_2(
        arg00: Long = 0L, arg01: Long = 1L, arg02: Long = 2L, arg03: Long = 3L, arg04: Long = 4L,
        arg05: Long = 5L, arg06: Long = 6L, arg07: Long = 7L, arg08: Long = 8L, arg09: Long = 9L,
        arg10: Long = 10L, arg11: Long = 11L, arg12: Long = 12L, arg13: Long = 13L, arg14: Long = 14L,
        arg15: Long = 15L, arg16: Long = 16L, arg17: Long = 17L, arg18: Long = 18L, arg19: Long = 19L,
        arg20: Long = 20L, arg21: Long = 21L, arg22: Long = 22L, arg23: Long = 23L, arg24: Long = 24L,
        arg25: Long = 25L, arg26: Long = 26L, arg27: Long = 27L, arg28: Long = 28L, arg29: Long = 29L,
        arg30: Long = 30L, x: S? = default
    ): S = listOf(
        arg00, arg01, arg02, arg03, arg04, arg05, arg06, arg07, arg08, arg09,
        arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19,
        arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29,
        arg30
    ).sum().let { S(it.toInt()) } + x!!

    fun memberDefault33_1(
        arg00: Long = 0L, arg01: Long = 1L, arg02: Long = 2L, arg03: Long = 3L, arg04: Long = 4L,
        arg05: Long = 5L, arg06: Long = 6L, arg07: Long = 7L, arg08: Long = 8L, arg09: Long = 9L,
        arg10: Long = 10L, arg11: Long = 11L, arg12: Long = 12L, arg13: Long = 13L, arg14: Long = 14L,
        arg15: Long = 15L, arg16: Long = 16L, arg17: Long = 17L, arg18: Long = 18L, arg19: Long = 19L,
        arg20: Long = 20L, arg21: Long = 21L, arg22: Long = 22L, arg23: Long = 23L, arg24: Long = 24L,
        arg25: Long = 25L, arg26: Long = 26L, arg27: Long = 27L, arg28: Long = 28L, arg29: Long = 29L,
        arg30: Long = 30L, arg31: Long = 31L, x: S = default
    ): S = listOf(
        arg00, arg01, arg02, arg03, arg04, arg05, arg06, arg07, arg08, arg09,
        arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19,
        arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29,
        arg30, arg31
    ).sum().let { S(it.toInt()) } + x
    fun memberDefault33_2(
        arg00: Long = 0L, arg01: Long = 1L, arg02: Long = 2L, arg03: Long = 3L, arg04: Long = 4L,
        arg05: Long = 5L, arg06: Long = 6L, arg07: Long = 7L, arg08: Long = 8L, arg09: Long = 9L,
        arg10: Long = 10L, arg11: Long = 11L, arg12: Long = 12L, arg13: Long = 13L, arg14: Long = 14L,
        arg15: Long = 15L, arg16: Long = 16L, arg17: Long = 17L, arg18: Long = 18L, arg19: Long = 19L,
        arg20: Long = 20L, arg21: Long = 21L, arg22: Long = 22L, arg23: Long = 23L, arg24: Long = 24L,
        arg25: Long = 25L, arg26: Long = 26L, arg27: Long = 27L, arg28: Long = 28L, arg29: Long = 29L,
        arg30: Long = 30L, arg31: Long = 31L, x: S? = default
    ): S = listOf(
        arg00, arg01, arg02, arg03, arg04, arg05, arg06, arg07, arg08, arg09,
        arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19,
        arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29,
        arg30, arg31
    ).sum().let { S(it.toInt()) } + x!!
}

private fun <T> KCallable<T>.callBy(vararg args: Any?): T =
    callBy(parameters.associateWith { args[it.index] })

private fun <T> KCallable<T>.callByEmpty(instance: Any): T = callBy(mapOf(instanceParameter!! to instance))

fun box(): String {
    val zero = S(0)
    val one = S(1)
    val four = S(4)
    val seven = S(7)

    val instance = C()

    assertEquals(seven, C::member.callBy(instance, one, 2, four))
    assertEquals(seven, C::memberDefault1_1.callBy(instance, seven))
    assertEquals(default, C::memberDefault1_1.callByEmpty(instance))
    assertEquals(seven, C::memberDefault1_2.callBy(instance, seven))
    assertEquals(default, C::memberDefault1_2.callByEmpty(instance))
    assertEquals(
        S(0),
        C::memberDefault32_1.callBy(
            instance,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertEquals(instance.memberDefault32_1(), C::memberDefault32_1.callByEmpty(instance))
    assertEquals(
        S(0),
        C::memberDefault32_2.callBy(
            instance,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertEquals(instance.memberDefault32_2(), C::memberDefault32_2.callByEmpty(instance))
    assertEquals(
        S(0),
        C::memberDefault33_1.callBy(
            instance,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(instance.memberDefault33_1(), C::memberDefault33_1.callByEmpty(instance))
    assertEquals(
        S(0),
        C::memberDefault33_2.callBy(
            instance,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(instance.memberDefault33_2(), C::memberDefault33_2.callByEmpty(instance))

    assertEquals(seven, instance::member.callBy(one, 2, four))
    assertEquals(seven, instance::memberDefault1_1.callBy(seven))
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(default, instance::memberDefault1_1.callBy(emptyMap()))
    }
    assertEquals(seven, instance::memberDefault1_2.callBy(seven))
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(default, instance::memberDefault1_2.callBy(emptyMap()))
    }
    assertEquals(
        S(0),
        instance::memberDefault32_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(instance.memberDefault32_1(), instance::memberDefault32_1.callBy(emptyMap()))
    }
    assertEquals(
        S(0),
        instance::memberDefault32_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(instance.memberDefault32_2(), instance::memberDefault32_2.callBy(emptyMap()))
    }
    assertEquals(
        S(0),
        instance::memberDefault33_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(instance.memberDefault33_1(), instance::memberDefault33_1.callBy(emptyMap()))
    }
    assertEquals(
        S(0),
        instance::memberDefault33_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(instance.memberDefault33_2(), instance::memberDefault33_2.callBy(emptyMap()))
    }

    return "OK"
}
