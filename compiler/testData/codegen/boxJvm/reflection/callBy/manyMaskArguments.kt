// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

// Generate:
// (1..70).map { "        p${"%02d".format(it)}: Int = $it," }.joinToString("\n")

class A {
    fun foo(
            p01: Int = 1,
            p02: Int = 2,
            p03: Int = 3,
            p04: Int = 4,
            p05: Int = 5,
            p06: Int = 6,
            p07: Int = 7,
            p08: Int = 8,
            p09: Int = 9,
            p10: Int = 10,
            p11: Int = 11,
            p12: Int = 12,
            p13: Int = 13,
            p14: Int = 14,
            p15: Int = 15,
            p16: Int = 16,
            p17: Int = 17,
            p18: Int = 18,
            p19: Int = 19,
            p20: Int = 20,
            p21: Int = 21,
            p22: Int = 22,
            p23: Int = 23,
            p24: Int = 24,
            p25: Int = 25,
            p26: Int = 26,
            p27: Int = 27,
            p28: Int = 28,
            p29: Int = 29,
            p30: Int = 30,
            p31: Int = 31,
            p32: Int = 32,
            p33: Int = 33,
            p34: Int = 34,
            p35: Int = 35,
            p36: Int = 36,
            p37: Int = 37,
            p38: Int = 38,
            p39: Int = 39,
            p40: Int = 40,
            p41: Int = 41,
            p42: Int,
            p43: Int = 43,
            p44: Int = 44,
            p45: Int = 45,
            p46: Int = 46,
            p47: Int = 47,
            p48: Int = 48,
            p49: Int = 49,
            p50: Int = 50,
            p51: Int = 51,
            p52: Int = 52,
            p53: Int = 53,
            p54: Int = 54,
            p55: Int = 55,
            p56: Int = 56,
            p57: Int = 57,
            p58: Int = 58,
            p59: Int = 59,
            p60: Int = 60,
            p61: Int = 61,
            p62: Int = 62,
            p63: Int = 63,
            p64: Int = 64,
            p65: Int = 65,
            p66: Int = 66,
            p67: Int = 67,
            p68: Int = 68,
            p69: Int = 69,
            p70: Int = 70
    ) {
        assertEquals(1, p01)
        assertEquals(41, p41)
        assertEquals(239, p42)
        assertEquals(43, p43)
        assertEquals(70, p70)
    }
}

fun box(): String {
    val f = A::class.members.single { it.name == "foo" }
    val parameters = f.parameters
    f.callBy(mapOf(
            parameters.first() to A(),
            parameters.single { it.name == "p42" } to 239
    ))
    return "OK"
}
