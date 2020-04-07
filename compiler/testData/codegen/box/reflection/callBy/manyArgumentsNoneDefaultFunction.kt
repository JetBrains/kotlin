// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import kotlin.test.assertEquals

// Generate:
// (1..70).map { "    p${"%02d".format(it)}: Int," }.joinToString("\n")

class A {
    fun foo(
            p01: Int,
            p02: Int,
            p03: Int,
            p04: Int,
            p05: Int,
            p06: Int,
            p07: Int,
            p08: Int,
            p09: Int,
            p10: Int,
            p11: Int,
            p12: Int,
            p13: Int,
            p14: Int,
            p15: Int,
            p16: Int,
            p17: Int,
            p18: Int,
            p19: Int,
            p20: Int,
            p21: Int,
            p22: Int,
            p23: Int,
            p24: Int,
            p25: Int,
            p26: Int,
            p27: Int,
            p28: Int,
            p29: Int,
            p30: Int,
            p31: Int,
            p32: Int,
            p33: Int,
            p34: Int,
            p35: Int,
            p36: Int,
            p37: Int,
            p38: Int,
            p39: Int,
            p40: Int,
            p41: Int,
            p42: Int,
            p43: Int,
            p44: Int,
            p45: Int,
            p46: Int,
            p47: Int,
            p48: Int,
            p49: Int,
            p50: Int,
            p51: Int,
            p52: Int,
            p53: Int,
            p54: Int,
            p55: Int,
            p56: Int,
            p57: Int,
            p58: Int,
            p59: Int,
            p60: Int,
            p61: Int,
            p62: Int,
            p63: Int,
            p64: Int,
            p65: Int,
            p66: Int,
            p67: Int,
            p68: Int,
            p69: Int,
            p70: Int
    ) {
        assertEquals(1, p01)
        assertEquals(41, p41)
        assertEquals(42, p42)
        assertEquals(43, p43)
        assertEquals(70, p70)
    }
}

fun box(): String {
    val f = A::class.members.single { it.name == "foo" }
    val parameters = f.parameters

    f.callBy(mapOf(
            parameters.first() to A(),
            *((1..70)).map { i -> parameters[i] to i }.toTypedArray()
    ))

    return "OK"
}
