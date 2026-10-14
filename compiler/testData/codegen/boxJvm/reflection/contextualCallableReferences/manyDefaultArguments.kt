// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals

context(c: String)
fun fits(
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
): String = c + (p01 + p02 + p03 + p04 + p05 + p06 + p07 + p08 + p09 + p10 + p11 + p12 + p13 + p14 + p15 + p16 + p17 + p18 + p19 + p20 + p21 + p22 + p23 + p24 + p25 + p26 + p27 + p28 + p29 + p30 + p31)

context(c: String)
fun overflows(
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
): String = c + (p01 + p02 + p03 + p04 + p05 + p06 + p07 + p08 + p09 + p10 + p11 + p12 + p13 + p14 + p15 + p16 + p17 + p18 + p19 + p20 + p21 + p22 + p23 + p24 + p25 + p26 + p27 + p28 + p29 + p30 + p31 + p32)

private fun checkBound(f: KFunction<String>, sum: Int) {
    assertEquals(List(f.parameters.size) { KParameter.Kind.VALUE }, f.parameters.map { it.kind })
    val first = f.parameters.first()
    val last = f.parameters.last()
    assertEquals("ctx$sum", f.callBy(emptyMap()))
    assertEquals("ctx${sum - 1}", f.callBy(mapOf(first to 0)))
    assertEquals("ctx${sum - last.index - 1}", f.callBy(mapOf(last to 0)))
}

private fun checkUnbound(f: KFunction<String>, sum: Int) {
    assertEquals(KParameter.Kind.CONTEXT, f.parameters.first().kind)
    val context = f.parameters.first()
    val first = f.parameters[1]
    val last = f.parameters.last()
    assertEquals("ctx$sum", f.callBy(mapOf(context to "ctx")))
    assertEquals("ctx${sum - 1}", f.callBy(mapOf(context to "ctx", first to 0)))
    assertEquals("ctx${sum - last.index}", f.callBy(mapOf(context to "ctx", last to 0)))
}

fun box(): String {
    context("ctx") {
        val fits = ::fits
        val overflows = ::overflows

        checkBound(fits, 496)
        checkBound(overflows, 528)

        checkUnbound(fits.javaMethod!!.kotlinFunction!! as KFunction<String>, 496)
        checkUnbound(overflows.javaMethod!!.kotlinFunction!! as KFunction<String>, 528)
    }
    return "OK"
}
