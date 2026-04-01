// TARGET_BACKEND: JVM

// LANGUAGE: +ContextParameters
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.test.assertEquals
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction


context(x: String, y: String)
fun topLevelFoo30(
    p1: Double = 1.0,
    p2: Double = 2.0,
    p3: Double = 3.0,
    p4: Double = 4.0,
    p5: Double = 5.0,
    p6: Double = 6.0,
    p7: Double = 7.0,
    p8: Double = 8.0,
    p9: Double = 9.0,
    p10: Double = 10.0,
    p11: Double = 11.0,
    p12: Double = 12.0,
    p13: Double = 13.0,
    p14: Double = 14.0,
    p15: Double = 15.0,
    p16: Double = 16.0,
    p17: Double = 17.0,
    p18: Double = 18.0,
    p19: Double = 19.0,
    p20: Double = 20.0,
    p21: Double = 21.0,
    p22: Double = 22.0,
    p23: Double = 23.0,
    p24: Double = 24.0,
    p25: Double = 25.0,
    p26: Double = 26.0,
    p27: Double = 27.0,
    p28: Double = 28.0,
    p29: Double = 29.0,
    p30: Double = 30.0,
) = "${x}_${y}_${p1}_${p10}_${p30}"

context(x: String, y: String)
fun topLevelFoo31(
    p1: Double = 1.0,
    p2: Double = 2.0,
    p3: Double = 3.0,
    p4: Double = 4.0,
    p5: Double = 5.0,
    p6: Double = 6.0,
    p7: Double = 7.0,
    p8: Double = 8.0,
    p9: Double = 9.0,
    p10: Double = 10.0,
    p11: Double = 11.0,
    p12: Double = 12.0,
    p13: Double = 13.0,
    p14: Double = 14.0,
    p15: Double = 15.0,
    p16: Double = 16.0,
    p17: Double = 17.0,
    p18: Double = 18.0,
    p19: Double = 19.0,
    p20: Double = 20.0,
    p21: Double = 21.0,
    p22: Double = 22.0,
    p23: Double = 23.0,
    p24: Double = 24.0,
    p25: Double = 25.0,
    p26: Double = 26.0,
    p27: Double = 27.0,
    p28: Double = 28.0,
    p29: Double = 29.0,
    p30: Double = 30.0,
    p31: Double = 31.0,
) = "${x}_${y}_${p1}_${p10}_${p31}"

context(x: String, y: String)
fun topLevelFoo32(
    p1: Double = 1.0,
    p2: Double = 2.0,
    p3: Double = 3.0,
    p4: Double = 4.0,
    p5: Double = 5.0,
    p6: Double = 6.0,
    p7: Double = 7.0,
    p8: Double = 8.0,
    p9: Double = 9.0,
    p10: Double = 10.0,
    p11: Double = 11.0,
    p12: Double = 12.0,
    p13: Double = 13.0,
    p14: Double = 14.0,
    p15: Double = 15.0,
    p16: Double = 16.0,
    p17: Double = 17.0,
    p18: Double = 18.0,
    p19: Double = 19.0,
    p20: Double = 20.0,
    p21: Double = 21.0,
    p22: Double = 22.0,
    p23: Double = 23.0,
    p24: Double = 24.0,
    p25: Double = 25.0,
    p26: Double = 26.0,
    p27: Double = 27.0,
    p28: Double = 28.0,
    p29: Double = 29.0,
    p30: Double = 30.0,
    p31: Double = 31.0,
    p32: Double = 32.0,
) = "${x}_${y}_${p1}_${p10}_${p32}"

context(x: String, y: String)
fun topLevelFoo33(
    p1: Double = 1.0,
    p2: Double = 2.0,
    p3: Double = 3.0,
    p4: Double = 4.0,
    p5: Double = 5.0,
    p6: Double = 6.0,
    p7: Double = 7.0,
    p8: Double = 8.0,
    p9: Double = 9.0,
    p10: Double = 10.0,
    p11: Double = 11.0,
    p12: Double = 12.0,
    p13: Double = 13.0,
    p14: Double = 14.0,
    p15: Double = 15.0,
    p16: Double = 16.0,
    p17: Double = 17.0,
    p18: Double = 18.0,
    p19: Double = 19.0,
    p20: Double = 20.0,
    p21: Double = 21.0,
    p22: Double = 22.0,
    p23: Double = 23.0,
    p24: Double = 24.0,
    p25: Double = 25.0,
    p26: Double = 26.0,
    p27: Double = 27.0,
    p28: Double = 28.0,
    p29: Double = 29.0,
    p30: Double = 30.0,
    p31: Double = 31.0,
    p32: Double = 32.0,
    p33: Double = 33.0,
) = "${x}_${y}_${p1}_${p10}_${p33}"

fun findParam(callable: KCallable<*>, name: String): KParameter = callable.parameters.single { it.name == name }
fun findFunc(name: String) =
    object {}::class.java.enclosingClass.declaredMethods.single { it.name == name }.kotlinFunction!!


fun box(): String {
    val topLevelFoo30Ref = findFunc("topLevelFoo30")
    val topLevelFoo30x = findParam(topLevelFoo30Ref, "x")
    val topLevelFoo30y = findParam(topLevelFoo30Ref, "y")
    val topLevelFoo30p1 = findParam(topLevelFoo30Ref, "p1")
    val topLevelFoo30p10 = findParam(topLevelFoo30Ref, "p10")
    val topLevelFoo30p30 = findParam(topLevelFoo30Ref, "p30")


    assertEquals(
        "a_b_42.0_10.0_30.0",
        topLevelFoo30Ref.callBy(
            mapOf(
                topLevelFoo30x to "a",
                topLevelFoo30y to "b",
                topLevelFoo30p1 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_42.0_30.0",
        topLevelFoo30Ref.callBy(
            mapOf(
                topLevelFoo30x to "a",
                topLevelFoo30y to "b",
                topLevelFoo30p10 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_10.0_42.0",
        topLevelFoo30Ref.callBy(
            mapOf(
                topLevelFoo30x to "a",
                topLevelFoo30y to "b",
                topLevelFoo30p30 to 42.0
            )
        )
    )

    val topLevelFoo31Ref = findFunc("topLevelFoo31")
    val topLevelFoo31x = findParam(topLevelFoo31Ref, "x")
    val topLevelFoo31y = findParam(topLevelFoo31Ref, "y")
    val topLevelFoo31p1 = findParam(topLevelFoo31Ref, "p1")
    val topLevelFoo31p10 = findParam(topLevelFoo31Ref, "p10")
    val topLevelFoo31p31 = findParam(topLevelFoo31Ref, "p31")

    assertEquals(
        "a_b_42.0_10.0_31.0",
        topLevelFoo31Ref.callBy(
            mapOf(
                topLevelFoo31x to "a",
                topLevelFoo31y to "b",
                topLevelFoo31p1 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_42.0_31.0",
        topLevelFoo31Ref.callBy(
            mapOf(
                topLevelFoo31x to "a",
                topLevelFoo31y to "b",
                topLevelFoo31p10 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_10.0_42.0",
        topLevelFoo31Ref.callBy(
            mapOf(
                topLevelFoo31x to "a",
                topLevelFoo31y to "b",
                topLevelFoo31p31 to 42.0
            )
        )
    )

    val topLevelFoo32Ref = findFunc("topLevelFoo32")
    val topLevelFoo32x = findParam(topLevelFoo32Ref, "x")
    val topLevelFoo32y = findParam(topLevelFoo32Ref, "y")
    val topLevelFoo32p1 = findParam(topLevelFoo32Ref, "p1")
    val topLevelFoo32p10 = findParam(topLevelFoo32Ref, "p10")
    val topLevelFoo32p32 = findParam(topLevelFoo32Ref, "p32")

    assertEquals(
        "a_b_42.0_10.0_32.0",
        topLevelFoo32Ref.callBy(
            mapOf(
                topLevelFoo32x to "a",
                topLevelFoo32y to "b",
                topLevelFoo32p1 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_42.0_32.0",
        topLevelFoo32Ref.callBy(
            mapOf(
                topLevelFoo32x to "a",
                topLevelFoo32y to "b",
                topLevelFoo32p10 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_10.0_42.0",
        topLevelFoo32Ref.callBy(
            mapOf(
                topLevelFoo32x to "a",
                topLevelFoo32y to "b",
                topLevelFoo32p32 to 42.0
            )
        )
    )

    val topLevelFoo33Ref = findFunc("topLevelFoo33")
    val topLevelFoo33x = findParam(topLevelFoo33Ref, "x")
    val topLevelFoo33y = findParam(topLevelFoo33Ref, "y")
    val topLevelFoo33p1 = findParam(topLevelFoo33Ref, "p1")
    val topLevelFoo33p10 = findParam(topLevelFoo33Ref, "p10")
    val topLevelFoo33p33 = findParam(topLevelFoo33Ref, "p33")

    assertEquals(
        "a_b_42.0_10.0_33.0",
        topLevelFoo33Ref.callBy(
            mapOf(
                topLevelFoo33x to "a",
                topLevelFoo33y to "b",
                topLevelFoo33p1 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_42.0_33.0",
        topLevelFoo33Ref.callBy(
            mapOf(
                topLevelFoo33x to "a",
                topLevelFoo33y to "b",
                topLevelFoo33p10 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_10.0_42.0",
        topLevelFoo33Ref.callBy(
            mapOf(
                topLevelFoo33x to "a",
                topLevelFoo33y to "b",
                topLevelFoo33p33 to 42.0
            )
        )
    )

    return "OK"
}