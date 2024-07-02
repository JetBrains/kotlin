// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@JvmInline
value class S(val value: String?) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

val default = S("-default")

interface I {
    companion object {
        @JvmStatic
        fun bar(x: S, y: String, z: S?): S = x + S(y) + z!!

        @JvmStatic
        fun staticDefault1_1(x: S = default): S = x
        @JvmStatic
        fun staticDefault1_2(x: S? = default): S = x!!

        @JvmStatic
        fun staticDefault32_1(
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
        ).sum().let { S(it.toString()) } + x
        @JvmStatic
        fun staticDefault32_2(
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
        ).sum().let { S(it.toString()) } + x!!

        @JvmStatic
        fun staticDefault33_1(
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
        ).sum().let { S(it.toString()) } + x
        @JvmStatic
        fun staticDefault33_2(
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
        ).sum().let { S(it.toString()) } + x!!
    }
}

private fun <T> KCallable<T>.callBy(vararg args: Any?): T {
    val params = parameters
    return params.fold(HashMap<KParameter, Any?>()) { acc, cur ->
        acc.apply {
            acc[cur] = args[cur.index]
        }
    }.let { callBy(it) }
}

private fun <T> KCallable<T>.callByEmpty(instance: Any): T = callBy(mapOf(instanceParameter!! to instance))

fun box(): String {
    val zero = S("0")
    val one = S("1")
    val four = S("4")

    val cMembers = I.Companion::class.members.associateBy { it.name }
    assertEquals(S("124"), cMembers.getValue("bar").callBy(I, one, "2", four))
    assertEquals(four, cMembers.getValue("staticDefault1_1").callBy(I, four))
    assertEquals(default, cMembers.getValue("staticDefault1_1").callByEmpty(I))
    assertEquals(four, cMembers.getValue("staticDefault1_2").callBy(I, four))
    assertEquals(default, cMembers.getValue("staticDefault1_2").callByEmpty(I))
    assertEquals(
        S("00"),
        cMembers.getValue("staticDefault32_1").callBy(
            I,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertEquals(I.staticDefault32_1(), cMembers.getValue("staticDefault32_1").callByEmpty(I))
    assertEquals(
        S("00"),
        cMembers.getValue("staticDefault32_2").callBy(
            I,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertEquals(I.staticDefault32_2(), cMembers.getValue("staticDefault32_2").callByEmpty(I))
    assertEquals(
        S("00"),
        cMembers.getValue("staticDefault33_1").callBy(
            I,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(I.staticDefault33_1(), cMembers.getValue("staticDefault33_1").callByEmpty(I))
    assertEquals(
        S("00"),
        cMembers.getValue("staticDefault33_2").callBy(
            I,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(I.staticDefault33_2(), cMembers.getValue("staticDefault33_2").callByEmpty(I))

    assertEquals(S("124"), I.Companion::bar.callBy(one, "2", four))
    assertEquals(four, I.Companion::staticDefault1_1.callBy(four))
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(default, I.Companion::staticDefault1_1.callBy(emptyMap()))
    }
    assertEquals(four, I.Companion::staticDefault1_2.callBy(four))
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(default, I.Companion::staticDefault1_2.callBy(emptyMap()))
    }
    assertEquals(
        S("00"),
        I.Companion::staticDefault32_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(I.staticDefault32_1(), I.Companion::staticDefault32_1.callBy(emptyMap()))
    }
    assertEquals(
        S("00"),
        I.Companion::staticDefault32_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(I.staticDefault32_2(), I.Companion::staticDefault32_2.callBy(emptyMap()))
    }
    assertEquals(
        S("00"),
        I.Companion::staticDefault33_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(I.staticDefault33_1(), I.Companion::staticDefault33_1.callBy(emptyMap()))
    }
    assertEquals(
        S("00"),
        I.Companion::staticDefault33_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(I.staticDefault33_2(), I.Companion::staticDefault33_2.callBy(emptyMap()))
    }

    return "OK"
}
