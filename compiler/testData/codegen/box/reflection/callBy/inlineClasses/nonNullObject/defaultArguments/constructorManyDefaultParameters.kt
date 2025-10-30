// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.test.assertEquals


@JvmInline
value class A(val x: String)

data class A30(
    val p1: A = A("1"),
    val p2: A = A("2"),
    val p3: A = A("3"),
    val p4: A = A("4"),
    val p5: A = A("5"),
    val p6: A = A("6"),
    val p7: A = A("7"),
    val p8: A = A("8"),
    val p9: A = A("9"),
    val p10: A = A("10"),
    val p11: A = A("11"),
    val p12: A = A("12"),
    val p13: A = A("13"),
    val p14: A = A("14"),
    val p15: A = A("15"),
    val p16: A = A("16"),
    val p17: A = A("17"),
    val p18: A = A("18"),
    val p19: A = A("19"),
    val p20: A = A("20"),
    val p21: A = A("21"),
    val p22: A = A("22"),
    val p23: A = A("23"),
    val p24: A = A("24"),
    val p25: A = A("25"),
    val p26: A = A("26"),
    val p27: A = A("27"),
    val p28: A = A("28"),
    val p29: A = A("29"),
    val p30: A = A("30")
)

data class A33(
    val p1: A = A("1"),
    val p2: A = A("2"),
    val p3: A = A("3"),
    val p4: A = A("4"),
    val p5: A = A("5"),
    val p6: A = A("6"),
    val p7: A = A("7"),
    val p8: A = A("8"),
    val p9: A = A("9"),
    val p10: A = A("10"),
    val p11: A = A("11"),
    val p12: A = A("12"),
    val p13: A = A("13"),
    val p14: A = A("14"),
    val p15: A = A("15"),
    val p16: A = A("16"),
    val p17: A = A("17"),
    val p18: A = A("18"),
    val p19: A = A("19"),
    val p20: A = A("20"),
    val p21: A = A("21"),
    val p22: A = A("22"),
    val p23: A = A("23"),
    val p24: A = A("24"),
    val p25: A = A("25"),
    val p26: A = A("26"),
    val p27: A = A("27"),
    val p28: A = A("28"),
    val p29: A = A("29"),
    val p30: A = A("30"),
    val p31: A = A("31"),
    val p32: A = A("32"),
    val p33: A = A("33")
)

fun findParam(callable: KCallable<*>, name: String) = callable.parameters.single { it.name == name }


fun box(): String {
    assertEquals(
        A30(p1 = A("x"), p10 = A("y"), p30 = A("z")),
        ::A30.callBy(
            mapOf(
                findParam(::A30, "p1") to A("x"),
                findParam(::A30, "p10") to A("y"),
                findParam(::A30, "p30") to A("z")
            )
        )
    )

    assertEquals(
        A33(p1 = A("x"), p10 = A("y"), p33 = A("z")),
        ::A33.callBy(
            mapOf(
                findParam(::A33, "p1") to A("x"),
                findParam(::A33, "p10") to A("y"),
                findParam(::A33, "p33") to A("z")
            )
        )
    )

    return "OK"
}