// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

inline class A(val x: Int)

fun test1(x: A = A(0)) = "OK"

fun test32(
    arg00: Long = 0L, arg01: Long = 0L, arg02: Long = 0L, arg03: Long = 0L, arg04: Long = 0L,
    arg05: Long = 0L, arg06: Long = 0L, arg07: Long = 0L, arg08: Long = 0L, arg09: Long = 0L,
    arg10: Long = 0L, arg11: Long = 0L, arg12: Long = 0L, arg13: Long = 0L, arg14: Long = 0L,
    arg15: Long = 0L, arg16: Long = 0L, arg17: Long = 0L, arg18: Long = 0L, arg19: Long = 0L,
    arg20: Long = 0L, arg21: Long = 0L, arg22: Long = 0L, arg23: Long = 0L, arg24: Long = 0L,
    arg25: Long = 0L, arg26: Long = 0L, arg27: Long = 0L, arg28: Long = 0L, arg29: Long = 0L,
    arg30: Long = 0L, x: A = A(0)
) = "OK"

fun test33(
    arg00: Long = 0L, arg01: Long = 0L, arg02: Long = 0L, arg03: Long = 0L, arg04: Long = 0L,
    arg05: Long = 0L, arg06: Long = 0L, arg07: Long = 0L, arg08: Long = 0L, arg09: Long = 0L,
    arg10: Long = 0L, arg11: Long = 0L, arg12: Long = 0L, arg13: Long = 0L, arg14: Long = 0L,
    arg15: Long = 0L, arg16: Long = 0L, arg17: Long = 0L, arg18: Long = 0L, arg19: Long = 0L,
    arg20: Long = 0L, arg21: Long = 0L, arg22: Long = 0L, arg23: Long = 0L, arg24: Long = 0L,
    arg25: Long = 0L, arg26: Long = 0L, arg27: Long = 0L, arg28: Long = 0L, arg29: Long = 0L,
    arg30: Long = 0L, arg31: Long = 0L, x: A = A(0)
) = "OK"

fun test64(
    arg00: Long = 0L, arg01: Long = 0L, arg02: Long = 0L, arg03: Long = 0L, arg04: Long = 0L,
    arg05: Long = 0L, arg06: Long = 0L, arg07: Long = 0L, arg08: Long = 0L, arg09: Long = 0L,
    arg10: Long = 0L, arg11: Long = 0L, arg12: Long = 0L, arg13: Long = 0L, arg14: Long = 0L,
    arg15: Long = 0L, arg16: Long = 0L, arg17: Long = 0L, arg18: Long = 0L, arg19: Long = 0L,
    arg20: Long = 0L, arg21: Long = 0L, arg22: Long = 0L, arg23: Long = 0L, arg24: Long = 0L,
    arg25: Long = 0L, arg26: Long = 0L, arg27: Long = 0L, arg28: Long = 0L, arg29: Long = 0L,
    arg30: Long = 0L, arg31: Long = 0L, arg32: Long = 0L, arg33: Long = 0L, arg34: Long = 0L,
    arg35: Long = 0L, arg36: Long = 0L, arg37: Long = 0L, arg38: Long = 0L, arg39: Long = 0L,
    arg40: Long = 0L, arg41: Long = 0L, arg42: Long = 0L, arg43: Long = 0L, arg44: Long = 0L,
    arg45: Long = 0L, arg46: Long = 0L, arg47: Long = 0L, arg48: Long = 0L, arg49: Long = 0L,
    arg50: Long = 0L, arg51: Long = 0L, arg52: Long = 0L, arg53: Long = 0L, arg54: Long = 0L,
    arg55: Long = 0L, arg56: Long = 0L, arg57: Long = 0L, arg58: Long = 0L, arg59: Long = 0L,
    arg60: Long = 0L, arg61: Long = 0L, arg62: Long = 0L, x: A = A(0)
) = "OK"

fun test65(
    arg00: Long = 0L, arg01: Long = 0L, arg02: Long = 0L, arg03: Long = 0L, arg04: Long = 0L,
    arg05: Long = 0L, arg06: Long = 0L, arg07: Long = 0L, arg08: Long = 0L, arg09: Long = 0L,
    arg10: Long = 0L, arg11: Long = 0L, arg12: Long = 0L, arg13: Long = 0L, arg14: Long = 0L,
    arg15: Long = 0L, arg16: Long = 0L, arg17: Long = 0L, arg18: Long = 0L, arg19: Long = 0L,
    arg20: Long = 0L, arg21: Long = 0L, arg22: Long = 0L, arg23: Long = 0L, arg24: Long = 0L,
    arg25: Long = 0L, arg26: Long = 0L, arg27: Long = 0L, arg28: Long = 0L, arg29: Long = 0L,
    arg30: Long = 0L, arg31: Long = 0L, arg32: Long = 0L, arg33: Long = 0L, arg34: Long = 0L,
    arg35: Long = 0L, arg36: Long = 0L, arg37: Long = 0L, arg38: Long = 0L, arg39: Long = 0L,
    arg40: Long = 0L, arg41: Long = 0L, arg42: Long = 0L, arg43: Long = 0L, arg44: Long = 0L,
    arg45: Long = 0L, arg46: Long = 0L, arg47: Long = 0L, arg48: Long = 0L, arg49: Long = 0L,
    arg50: Long = 0L, arg51: Long = 0L, arg52: Long = 0L, arg53: Long = 0L, arg54: Long = 0L,
    arg55: Long = 0L, arg56: Long = 0L, arg57: Long = 0L, arg58: Long = 0L, arg59: Long = 0L,
    arg60: Long = 0L, arg61: Long = 0L, arg62: Long = 0L, arg63: Long = 0L, x: A = A(0)
) = "OK"

fun box(): String {
    assertEquals("OK", ::test1.callBy(mapOf()))
    assertEquals("OK", ::test32.callBy(mapOf()))
    assertEquals("OK", ::test33.callBy(mapOf()))
    assertEquals("OK", ::test64.callBy(mapOf()))
    assertEquals("OK", ::test65.callBy(mapOf()))

    return "OK"
}
