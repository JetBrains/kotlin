// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

fun String.foo(
    p1: Double = 1.0, p2: Double = 1.0, p3: Double = 1.0, p4: Double = 1.0, p5: Double = 1.0, p6: Double = 1.0,
    p7: Double = 1.0, p8: Double = 1.0, p9: Double = 1.0, p10: Double = 1.0, p11: Double = 1.0, p12: Double = 1.0,
    p13: Double = 1.0, p14: Double = 1.0, p15: Double = 1.0, p16: Double = 1.0, p17: Double = 1.0, p18: Double = 1.0,
    p19: Double = 1.0, p20: Double = 1.0, p21: Double = 1.0, p22: Double = 1.0, p23: Double = 1.0, p24: Double = 1.0,
    p25: Double = 1.0, p26: Double = 1.0, p27: Double = 1.0, p28: Double = 1.0, p29: Double = 1.0, p30: Double = 1.0,
    p31: Double = 1.0, p32: Double = 1.0
) = "OK"

fun box(): String {
    val ref = String::foo
    val extParam = ref.parameters.first()
    val result = ref.callBy(mapOf(extParam to ""))
    assertEquals("OK", result)
    return result
}
