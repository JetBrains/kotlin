// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

@JvmInline
value class A(val x: String)

data class TestCtor1_1(val x: A = A("0"))
data class TestCtor1_2(val x: A? = A("0"))

data class TestCtor32_1(
    val arg00: Long = 0L, val arg01: Long = 0L, val arg02: Long = 0L, val arg03: Long = 0L, val arg04: Long = 0L,
    val arg05: Long = 0L, val arg06: Long = 0L, val arg07: Long = 0L, val arg08: Long = 0L, val arg09: Long = 0L,
    val arg10: Long = 0L, val arg11: Long = 0L, val arg12: Long = 0L, val arg13: Long = 0L, val arg14: Long = 0L,
    val arg15: Long = 0L, val arg16: Long = 0L, val arg17: Long = 0L, val arg18: Long = 0L, val arg19: Long = 0L,
    val arg20: Long = 0L, val arg21: Long = 0L, val arg22: Long = 0L, val arg23: Long = 0L, val arg24: Long = 0L,
    val arg25: Long = 0L, val arg26: Long = 0L, val arg27: Long = 0L, val arg28: Long = 0L, val arg29: Long = 0L,
    val arg30: Long = 0L, val x: A = A("0")
)
data class TestCtor32_2(
    val arg00: Long = 0L, val arg01: Long = 0L, val arg02: Long = 0L, val arg03: Long = 0L, val arg04: Long = 0L,
    val arg05: Long = 0L, val arg06: Long = 0L, val arg07: Long = 0L, val arg08: Long = 0L, val arg09: Long = 0L,
    val arg10: Long = 0L, val arg11: Long = 0L, val arg12: Long = 0L, val arg13: Long = 0L, val arg14: Long = 0L,
    val arg15: Long = 0L, val arg16: Long = 0L, val arg17: Long = 0L, val arg18: Long = 0L, val arg19: Long = 0L,
    val arg20: Long = 0L, val arg21: Long = 0L, val arg22: Long = 0L, val arg23: Long = 0L, val arg24: Long = 0L,
    val arg25: Long = 0L, val arg26: Long = 0L, val arg27: Long = 0L, val arg28: Long = 0L, val arg29: Long = 0L,
    val arg30: Long = 0L, val x: A? = A("0")
)

data class TestCtor33_1(
    val arg00: Long = 0L, val arg01: Long = 0L, val arg02: Long = 0L, val arg03: Long = 0L, val arg04: Long = 0L,
    val arg05: Long = 0L, val arg06: Long = 0L, val arg07: Long = 0L, val arg08: Long = 0L, val arg09: Long = 0L,
    val arg10: Long = 0L, val arg11: Long = 0L, val arg12: Long = 0L, val arg13: Long = 0L, val arg14: Long = 0L,
    val arg15: Long = 0L, val arg16: Long = 0L, val arg17: Long = 0L, val arg18: Long = 0L, val arg19: Long = 0L,
    val arg20: Long = 0L, val arg21: Long = 0L, val arg22: Long = 0L, val arg23: Long = 0L, val arg24: Long = 0L,
    val arg25: Long = 0L, val arg26: Long = 0L, val arg27: Long = 0L, val arg28: Long = 0L, val arg29: Long = 0L,
    val arg30: Long = 0L, val arg31: Long = 0L, val x: A = A("0")
)
data class TestCtor33_2(
    val arg00: Long = 0L, val arg01: Long = 0L, val arg02: Long = 0L, val arg03: Long = 0L, val arg04: Long = 0L,
    val arg05: Long = 0L, val arg06: Long = 0L, val arg07: Long = 0L, val arg08: Long = 0L, val arg09: Long = 0L,
    val arg10: Long = 0L, val arg11: Long = 0L, val arg12: Long = 0L, val arg13: Long = 0L, val arg14: Long = 0L,
    val arg15: Long = 0L, val arg16: Long = 0L, val arg17: Long = 0L, val arg18: Long = 0L, val arg19: Long = 0L,
    val arg20: Long = 0L, val arg21: Long = 0L, val arg22: Long = 0L, val arg23: Long = 0L, val arg24: Long = 0L,
    val arg25: Long = 0L, val arg26: Long = 0L, val arg27: Long = 0L, val arg28: Long = 0L, val arg29: Long = 0L,
    val arg30: Long = 0L, val arg31: Long = 0L, val x: A? = A("0")
)

data class TestCtor64_1(
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
    val arg60: Long = 0L, val arg61: Long = 0L, val arg62: Long = 0L, val x: A = A("0")
)
data class TestCtor64_2(
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
    val arg60: Long = 0L, val arg61: Long = 0L, val arg62: Long = 0L, val x: A? = A("0")
)

data class TestCtor65_1(
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
    val arg60: Long = 0L, val arg61: Long = 0L, val arg62: Long = 0L, val arg63: Long = 0L, val x: A = A("0")
)
data class TestCtor65_2(
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
    val arg60: Long = 0L, val arg61: Long = 0L, val arg62: Long = 0L, val arg63: Long = 0L, val x: A? = A("0")
)

fun box(): String {
    assertEquals(TestCtor1_1(), ::TestCtor1_1.callBy(mapOf()))
    assertEquals(TestCtor1_2(), ::TestCtor1_2.callBy(mapOf()))
    assertEquals(TestCtor32_1(), ::TestCtor32_1.callBy(mapOf()))
    assertEquals(TestCtor32_2(), ::TestCtor32_2.callBy(mapOf()))
    assertEquals(TestCtor33_1(), ::TestCtor33_1.callBy(mapOf()))
    assertEquals(TestCtor33_2(), ::TestCtor33_2.callBy(mapOf()))
    assertEquals(TestCtor64_1(), ::TestCtor64_1.callBy(mapOf()))
    assertEquals(TestCtor64_2(), ::TestCtor64_2.callBy(mapOf()))
    assertEquals(TestCtor65_1(), ::TestCtor65_1.callBy(mapOf()))
    assertEquals(TestCtor65_2(), ::TestCtor65_2.callBy(mapOf()))

    return "OK"
}
