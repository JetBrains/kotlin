// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@JvmInline
value class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C

val default = S("-default")

fun S.extension1_0(): S = this
fun S?.extension2_0(): S = this!!

fun C.extensionDefault1_1(x: S = default): S = x
fun C.extensionDefault1_2(x: S? = default): S = x!!

fun S.extensionDefault1_1_1(x: S = default): S = this + x
fun S.extensionDefault1_1_2(x: S? = default): S = this + x!!
fun S?.extensionDefault2_1_1(x: S = default): S = this!! + x
fun S?.extensionDefault2_1_2(x: S? = default): S = this!! + x!!

fun C.extensionDefault32_1(
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
fun C.extensionDefault32_2(
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

fun S.extensionDefault1_32_1(
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
).sum().let { this + S(it.toString()) } + x
fun S.extensionDefault1_32_2(
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
).sum().let { this + S(it.toString()) } + x!!

fun S?.extensionDefault2_32_1(
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
).sum().let { this!! + S(it.toString()) } + x
fun S?.extensionDefault2_32_2(
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
).sum().let { this!! + S(it.toString()) } + x!!

fun C.extensionDefault33_1(
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
fun C.extensionDefault33_2(
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

fun S.extensionDefault1_33_1(
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
).sum().let { this + S(it.toString()) } + x
fun S.extensionDefault1_33_2(
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
).sum().let { this + S(it.toString()) } + x!!

fun S?.extensionDefault2_33_1(
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
).sum().let { this!! + S(it.toString()) } + x
fun S?.extensionDefault2_33_2(
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
).sum().let { this!! + S(it.toString()) } + x!!

private fun <T> KCallable<T>.callBy(vararg args: Any?): T =
    callBy(parameters.associateWith { args[it.index] })

private fun <T> KCallable<T>.callByEmpty(instance: Any): T = callBy(mapOf(extensionReceiverParameter!! to instance))

fun box(): String {
    val zero = S("0")
    val one = S("1")
    val two = S("2")

    val c = C()

    assertEquals(one, S::extension1_0.callBy(one))
    assertEquals(one, S::extension2_0.callBy(one))
    assertEquals(one, C::extensionDefault1_1.callBy(c, one))
    assertEquals(default, C::extensionDefault1_1.callByEmpty(c))
    assertEquals(one, C::extensionDefault1_2.callBy(c, one))
    assertEquals(default, C::extensionDefault1_2.callByEmpty(c))
    assertEquals(S("12"), S::extensionDefault1_1_1.callBy(one, two))
    assertEquals(S("1-default"), S::extensionDefault1_1_1.callByEmpty(one))
    assertEquals(S("12"), S::extensionDefault1_1_2.callBy(one, two))
    assertEquals(S("1-default"), S::extensionDefault1_1_2.callByEmpty(one))
    assertEquals(S("12"), S::extensionDefault2_1_1.callBy(one, two))
    assertEquals(S("1-default"), S::extensionDefault2_1_1.callByEmpty(one))
    assertEquals(S("12"), S::extensionDefault2_1_2.callBy(one, two))
    assertEquals(S("1-default"), S::extensionDefault2_1_2.callByEmpty(one))
    assertEquals(
        S("00"),
        C::extensionDefault32_1.callBy(
            c,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(c.extensionDefault32_1(), C::extensionDefault32_1.callByEmpty(c))
    }
    assertEquals(
        S("00"),
        C::extensionDefault32_2.callBy(
            c,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(c.extensionDefault32_2(), C::extensionDefault32_2.callByEmpty(c))
    }
    assertEquals(
        S("000"),
        S::extensionDefault1_32_1.callBy(
            zero,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(zero.extensionDefault1_32_1(), S::extensionDefault1_32_1.callByEmpty(zero))
    }
    assertEquals(
        S("000"),
        S::extensionDefault1_32_2.callBy(
            zero,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(zero.extensionDefault1_32_2(), S::extensionDefault1_32_2.callByEmpty(zero))
    }
    assertEquals(
        S("000"),
        S::extensionDefault2_32_1.callBy(
            zero,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(zero.extensionDefault2_32_1(), S::extensionDefault2_32_1.callByEmpty(zero))
    }
    assertEquals(
        S("000"),
        S::extensionDefault2_32_2.callBy(
            zero,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(zero.extensionDefault2_32_2(), S::extensionDefault2_32_2.callByEmpty(zero))
    }
    assertEquals(
        S("00"),
        C::extensionDefault33_1.callBy(
            c,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(c.extensionDefault33_1(), C::extensionDefault33_1.callByEmpty(c))
    assertEquals(
        S("00"),
        C::extensionDefault33_2.callBy(
            c,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(c.extensionDefault33_2(), C::extensionDefault33_2.callByEmpty(c))
    assertEquals(
        S("000"),
        S::extensionDefault1_33_1.callBy(
            zero,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(zero.extensionDefault1_33_1(), S::extensionDefault1_33_1.callByEmpty(zero))
    assertEquals(
        S("000"),
        S::extensionDefault1_33_2.callBy(
            zero,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(zero.extensionDefault1_33_2(), S::extensionDefault1_33_2.callByEmpty(zero))
    assertEquals(
        S("000"),
        S::extensionDefault2_33_1.callBy(
            zero,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(zero.extensionDefault2_33_1(), S::extensionDefault2_33_1.callByEmpty(zero))
    assertEquals(
        S("000"),
        S::extensionDefault2_33_2.callBy(
            zero,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(zero.extensionDefault2_33_2(), S::extensionDefault2_33_2.callByEmpty(zero))

    assertEquals(one, one::extension1_0.callBy())
    assertEquals(one, one::extension2_0.callBy())
    assertEquals(one, c::extensionDefault1_1.callBy(one))
    assertEquals(default, c::extensionDefault1_1.callBy(emptyMap()))
    assertEquals(one, c::extensionDefault1_2.callBy(one))
    assertEquals(default, c::extensionDefault1_2.callBy(emptyMap()))
    assertEquals(S("12"), one::extensionDefault1_1_1.callBy(two))
    assertEquals(S("1-default"), one::extensionDefault1_1_1.callBy(emptyMap()))
    assertEquals(S("12"), one::extensionDefault1_1_2.callBy(two))
    assertEquals(S("1-default"), one::extensionDefault1_1_2.callBy(emptyMap()))
    assertEquals(S("12"), one::extensionDefault2_1_1.callBy(two))
    assertEquals(S("1-default"), one::extensionDefault2_1_1.callBy(emptyMap()))
    assertEquals(S("12"), one::extensionDefault2_1_2.callBy(two))
    assertEquals(S("1-default"), one::extensionDefault2_1_2.callBy(emptyMap()))
    assertEquals(
        S("00"),
        c::extensionDefault32_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(c.extensionDefault32_1(), c::extensionDefault32_1.callBy(emptyMap()))
    }
    assertEquals(
        S("00"),
        c::extensionDefault32_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(c.extensionDefault32_2(), c::extensionDefault32_2.callBy(emptyMap()))
    }
    assertEquals(
        S("000"),
        zero::extensionDefault1_32_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(zero.extensionDefault1_32_1(), zero::extensionDefault1_32_1.callBy(emptyMap()))
    }
    assertEquals(
        S("000"),
        zero::extensionDefault1_32_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(zero.extensionDefault1_32_2(), zero::extensionDefault1_32_2.callBy(emptyMap()))
    }
    assertEquals(
        S("000"),
        zero::extensionDefault2_32_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(zero.extensionDefault2_32_1(), zero::extensionDefault2_32_1.callBy(emptyMap()))
    }
    assertEquals(
        S("000"),
        zero::extensionDefault2_32_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, zero
        )
    )
    assertFailsWith<Error>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(zero.extensionDefault2_32_2(), zero::extensionDefault2_32_2.callBy(emptyMap()))
    }
    assertEquals(
        S("00"),
        c::extensionDefault33_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(c.extensionDefault33_1(), c::extensionDefault33_1.callBy(emptyMap()))
    assertEquals(
        S("00"),
        c::extensionDefault33_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(c.extensionDefault33_2(), c::extensionDefault33_2.callBy(emptyMap()))
    assertEquals(
        S("000"),
        zero::extensionDefault1_33_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(zero.extensionDefault1_33_1(), zero::extensionDefault1_33_1.callBy(emptyMap()))
    assertEquals(
        S("000"),
        zero::extensionDefault1_33_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(zero.extensionDefault1_33_2(), zero::extensionDefault1_33_2.callBy(emptyMap()))
    assertEquals(
        S("000"),
        zero::extensionDefault2_33_1.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(zero.extensionDefault2_33_1(), zero::extensionDefault2_33_1.callBy(emptyMap()))
    assertEquals(
        S("000"),
        zero::extensionDefault2_33_2.callBy(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, zero
        )
    )
    assertEquals(zero.extensionDefault2_33_2(), zero::extensionDefault2_33_2.callBy(emptyMap()))

    return "OK"
}
