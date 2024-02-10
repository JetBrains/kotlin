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

data class TestCtor1(val x: A = A(0))

data class TestCtor32(
    val arg00: Long = 0L, val arg01: Long = 0L, val arg02: Long = 0L, val arg03: Long = 0L, val arg04: Long = 0L,
    val arg05: Long = 0L, val arg06: Long = 0L, val arg07: Long = 0L, val arg08: Long = 0L, val arg09: Long = 0L,
    val arg10: Long = 0L, val arg11: Long = 0L, val arg12: Long = 0L, val arg13: Long = 0L, val arg14: Long = 0L,
    val arg15: Long = 0L, val arg16: Long = 0L, val arg17: Long = 0L, val arg18: Long = 0L, val arg19: Long = 0L,
    val arg20: Long = 0L, val arg21: Long = 0L, val arg22: Long = 0L, val arg23: Long = 0L, val arg24: Long = 0L,
    val arg25: Long = 0L, val arg26: Long = 0L, val arg27: Long = 0L, val arg28: Long = 0L, val arg29: Long = 0L,
    val arg30: Long = 0L, val x: A = A(0)
)

data class TestCtor33(
    val arg00: Long = 0L, val arg01: Long = 0L, val arg02: Long = 0L, val arg03: Long = 0L, val arg04: Long = 0L,
    val arg05: Long = 0L, val arg06: Long = 0L, val arg07: Long = 0L, val arg08: Long = 0L, val arg09: Long = 0L,
    val arg10: Long = 0L, val arg11: Long = 0L, val arg12: Long = 0L, val arg13: Long = 0L, val arg14: Long = 0L,
    val arg15: Long = 0L, val arg16: Long = 0L, val arg17: Long = 0L, val arg18: Long = 0L, val arg19: Long = 0L,
    val arg20: Long = 0L, val arg21: Long = 0L, val arg22: Long = 0L, val arg23: Long = 0L, val arg24: Long = 0L,
    val arg25: Long = 0L, val arg26: Long = 0L, val arg27: Long = 0L, val arg28: Long = 0L, val arg29: Long = 0L,
    val arg30: Long = 0L, val arg31: Long = 0L, val x: A = A(0)
)

data class TestCtor64(
    val arg00: Long = 0L, val arg01: Long = 0L, val arg02: Long = 0L, val arg03: Long = 0L, val arg04: Long = 0L,
    val arg05: Long = 0L, val arg06: Long = 0L, val arg07: Long = 0L, val arg08: Long = 0L, val arg09: Long = 0L,
    val arg10: Long = 0L, val arg11: Long = 0L, val arg12: Long = 0L, val arg13: Long = 0L, val arg14: Long = 0L,
    val arg15: Long = 0L, val arg16: Long = 0L, val arg17: Long = 0L, val arg18: Long = 0L, val arg19: Long = 0L,
    val arg20: Long = 0L, val arg21: Long = 0L, val arg22: Long = 0L, val arg23: Long = 0L, val arg24: Long = 0L,
    val arg25: Long = 0L, val arg26: Long = 0L, val arg27: Long = 0L, val arg28: Long = 0L, val arg29: Long = 0L,
    val arg30: Long = 0L, val arg31: Long = 0L, val arg32: Long = 0L, val arg33: Long = 0L, val arg34: Long = 0L,
    val arg35: Long = 0L, val arg36: Long = 0L, val arg37: Long = 0L, val arg38: Long = 0L, val arg39: Long = 0L,
    val arg40: Long = 0L, val arg41: Long = 0L, val arg42: Long = 0L, val arg43: Long = 0L, val arg44: Long = 0L,
    val arg45: Long = 0L, val arg46: Long = 0L, val arg47: Long = 0L, val arg48: Long = 0L, val arg49: Long = 0L,
    val arg50: Long = 0L, val arg51: Long = 0L, val arg52: Long = 0L, val arg53: Long = 0L, val arg54: Long = 0L,
    val arg55: Long = 0L, val arg56: Long = 0L, val arg57: Long = 0L, val arg58: Long = 0L, val arg59: Long = 0L,
    val arg60: Long = 0L, val arg61: Long = 0L, val arg62: Long = 0L, val x: A = A(0)
)

data class TestCtor65(
    val arg00: Long = 0L, val arg01: Long = 0L, val arg02: Long = 0L, val arg03: Long = 0L, val arg04: Long = 0L,
    val arg05: Long = 0L, val arg06: Long = 0L, val arg07: Long = 0L, val arg08: Long = 0L, val arg09: Long = 0L,
    val arg10: Long = 0L, val arg11: Long = 0L, val arg12: Long = 0L, val arg13: Long = 0L, val arg14: Long = 0L,
    val arg15: Long = 0L, val arg16: Long = 0L, val arg17: Long = 0L, val arg18: Long = 0L, val arg19: Long = 0L,
    val arg20: Long = 0L, val arg21: Long = 0L, val arg22: Long = 0L, val arg23: Long = 0L, val arg24: Long = 0L,
    val arg25: Long = 0L, val arg26: Long = 0L, val arg27: Long = 0L, val arg28: Long = 0L, val arg29: Long = 0L,
    val arg30: Long = 0L, val arg31: Long = 0L, val arg32: Long = 0L, val arg33: Long = 0L, val arg34: Long = 0L,
    val arg35: Long = 0L, val arg36: Long = 0L, val arg37: Long = 0L, val arg38: Long = 0L, val arg39: Long = 0L,
    val arg40: Long = 0L, val arg41: Long = 0L, val arg42: Long = 0L, val arg43: Long = 0L, val arg44: Long = 0L,
    val arg45: Long = 0L, val arg46: Long = 0L, val arg47: Long = 0L, val arg48: Long = 0L, val arg49: Long = 0L,
    val arg50: Long = 0L, val arg51: Long = 0L, val arg52: Long = 0L, val arg53: Long = 0L, val arg54: Long = 0L,
    val arg55: Long = 0L, val arg56: Long = 0L, val arg57: Long = 0L, val arg58: Long = 0L, val arg59: Long = 0L,
    val arg60: Long = 0L, val arg61: Long = 0L, val arg62: Long = 0L, val arg63: Long = 0L, val x: A = A(0)
)

fun box(): String {
    assertEquals("OK", ::test1.callBy(mapOf()))
    assertEquals("OK", ::test32.callBy(mapOf()))
    assertEquals("OK", ::test33.callBy(mapOf()))
    assertEquals("OK", ::test64.callBy(mapOf()))
    assertEquals("OK", ::test65.callBy(mapOf()))

    assertEquals(TestCtor1(), ::TestCtor1.callBy(mapOf()))
    assertEquals(TestCtor32(), ::TestCtor32.callBy(mapOf()))
    assertEquals(TestCtor33(), ::TestCtor33.callBy(mapOf()))
    assertEquals(TestCtor64(), ::TestCtor64.callBy(mapOf()))
    assertEquals(TestCtor65(), ::TestCtor65.callBy(mapOf()))

    return "OK"
}
