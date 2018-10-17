// !LANGUAGE: +FunctionTypesWithBigArity
// IGNORE_BACKEND: JS_IR, JS, JVM_IR, NATIVE
// WITH_REFLECT

import kotlin.test.assertEquals

class A

data class BigDataClass(
    val p00: A, val p01: A, val p02: A, val p03: A, val p04: A, val p05: A, val p06: A, val p07: A, val p08: A, val p09: A,
    val p10: A, val p11: A, val p12: A, val p13: A, val p14: A, val p15: A, val p16: A, val p17: A, val p18: A, val p19: A,
    val p20: A, val p21: A, val p22: A, val p23: A, val p24: A, val p25: A, val p26: A, val p27: A, val p28: A, val p29: A
)

fun box(): String {
    assertEquals(
        "[null, p00, p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, " +
                "p15, p16, p17, p18, p19, p20, p21, p22, p23, p24, p25, p26, p27, p28, p29]",
        BigDataClass::copy.parameters.map { it.name }.toString()
    )
    return "OK"
}
