// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.test.assertEquals


@JvmInline
value class A(val x: String?)

fun foo30(
    p1: A = A("1"),
    p2: A = A("2"),
    p3: A = A("3"),
    p4: A = A("4"),
    p5: A = A("5"),
    p6: A = A("6"),
    p7: A = A("7"),
    p8: A = A("8"),
    p9: A = A("9"),
    p10: A = A("10"),
    p11: A = A("11"),
    p12: A = A("12"),
    p13: A = A("13"),
    p14: A = A("14"),
    p15: A = A("15"),
    p16: A = A("16"),
    p17: A = A("17"),
    p18: A = A("18"),
    p19: A = A("19"),
    p20: A = A("20"),
    p21: A = A("21"),
    p22: A = A("22"),
    p23: A = A("23"),
    p24: A = A("24"),
    p25: A = A("25"),
    p26: A = A("26"),
    p27: A = A("27"),
    p28: A = A("28"),
    p29: A = A("29"),
    p30: A = A("30")
) = listOf(
    p1,
    p2,
    p3,
    p4,
    p5,
    p6,
    p7,
    p8,
    p9,
    p10,
    p11,
    p12,
    p13,
    p14,
    p15,
    p16,
    p17,
    p18,
    p19,
    p20,
    p21,
    p22,
    p23,
    p24,
    p25,
    p26,
    p27,
    p28,
    p29,
    p30
).joinToString()

fun foo33(
    p1: A = A("1"),
    p2: A = A("2"),
    p3: A = A("3"),
    p4: A = A("4"),
    p5: A = A("5"),
    p6: A = A("6"),
    p7: A = A("7"),
    p8: A = A("8"),
    p9: A = A("9"),
    p10: A = A("10"),
    p11: A = A("11"),
    p12: A = A("12"),
    p13: A = A("13"),
    p14: A = A("14"),
    p15: A = A("15"),
    p16: A = A("16"),
    p17: A = A("17"),
    p18: A = A("18"),
    p19: A = A("19"),
    p20: A = A("20"),
    p21: A = A("21"),
    p22: A = A("22"),
    p23: A = A("23"),
    p24: A = A("24"),
    p25: A = A("25"),
    p26: A = A("26"),
    p27: A = A("27"),
    p28: A = A("28"),
    p29: A = A("29"),
    p30: A = A("30"),
    p31: A = A("31"),
    p32: A = A("32"),
    p33: A = A("33")
) = listOf(
    p1,
    p2,
    p3,
    p4,
    p5,
    p6,
    p7,
    p8,
    p9,
    p10,
    p11,
    p12,
    p13,
    p14,
    p15,
    p16,
    p17,
    p18,
    p19,
    p20,
    p21,
    p22,
    p23,
    p24,
    p25,
    p26,
    p27,
    p28,
    p29,
    p30,
    p31,
    p32,
    p33
).joinToString()

fun findParam(callable: KCallable<*>, name: String) = callable.parameters.single { it.name == name }

fun box(): String {
    assertEquals(
        foo30(p1 = A("x"), p10 = A("y"), p30 = A("z")),
        ::foo30.callBy(
            mapOf(
                findParam(::foo30, "p1") to A("x"),
                findParam(::foo30, "p10") to A("y"),
                findParam(::foo30, "p30") to A("z")
            )
        )
    )

    assertEquals(
        foo33(p1 = A("x"), p10 = A("y"), p33 = A("z")),
        ::foo33.callBy(
            mapOf(
                findParam(::foo33, "p1") to A("x"),
                findParam(::foo33, "p10") to A("y"),
                findParam(::foo33, "p33") to A("z")
            )
        )
    )

    return "OK"
}