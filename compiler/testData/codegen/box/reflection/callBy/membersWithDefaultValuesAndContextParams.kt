// TARGET_BACKEND: JVM

// LANGUAGE: +ContextParameters
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.reflect.KParameter

class A {
    context(x: String, y: String)
    fun memberFoo30(
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
    fun memberFoo31(
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
    fun memberFoo32(
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
    fun memberFoo33(
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
}

fun findParam(callable: KCallable<*>, name: String): KParameter = callable.parameters.single { it.name == name }
fun findInstanceParam(callable: KCallable<*>): KParameter = callable.parameters.single { it.kind == KParameter.Kind.INSTANCE }
fun findMember(clazz: KClass<*>, name: String) = clazz.members.single { it.name == name }


fun box(): String {
    val memberFoo30Ref = findMember(A::class, "memberFoo30")
    val memberFoo30this = findInstanceParam(memberFoo30Ref)
    val memberFoo30x = findParam(memberFoo30Ref, "x")
    val memberFoo30y = findParam(memberFoo30Ref, "y")
    val memberFoo30p1 = findParam(memberFoo30Ref, "p1")
    val memberFoo30p10 = findParam(memberFoo30Ref, "p10")
    val memberFoo30p30 = findParam(memberFoo30Ref, "p30")


    assertEquals(
        "a_b_42.0_10.0_30.0",
        memberFoo30Ref.callBy(
            mapOf(
                memberFoo30this to A(),
                memberFoo30x to "a",
                memberFoo30y to "b",
                memberFoo30p1 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_42.0_30.0",
        memberFoo30Ref.callBy(
            mapOf(
                memberFoo30this to A(),
                memberFoo30x to "a",
                memberFoo30y to "b",
                memberFoo30p10 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_10.0_42.0",
        memberFoo30Ref.callBy(
            mapOf(
                memberFoo30this to A(),
                memberFoo30x to "a",
                memberFoo30y to "b",
                memberFoo30p30 to 42.0
            )
        )
    )

    val memberFoo31Ref = findMember(A::class, "memberFoo31")
    val memberFoo31this = findInstanceParam(memberFoo31Ref)
    val memberFoo31x = findParam(memberFoo31Ref, "x")
    val memberFoo31y = findParam(memberFoo31Ref, "y")
    val memberFoo31p1 = findParam(memberFoo31Ref, "p1")
    val memberFoo31p10 = findParam(memberFoo31Ref, "p10")
    val memberFoo31p31 = findParam(memberFoo31Ref, "p31")

    assertEquals(
        "a_b_42.0_10.0_31.0",
        memberFoo31Ref.callBy(
            mapOf(
                memberFoo31this to A(),
                memberFoo31x to "a",
                memberFoo31y to "b",
                memberFoo31p1 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_42.0_31.0",
        memberFoo31Ref.callBy(
            mapOf(
                memberFoo31this to A(),
                memberFoo31x to "a",
                memberFoo31y to "b",
                memberFoo31p10 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_10.0_42.0",
        memberFoo31Ref.callBy(
            mapOf(
                memberFoo31this to A(),
                memberFoo31x to "a",
                memberFoo31y to "b",
                memberFoo31p31 to 42.0
            )
        )
    )

    val memberFoo32Ref = findMember(A::class, "memberFoo32")
    val memberFoo32this = findInstanceParam(memberFoo32Ref)
    val memberFoo32x = findParam(memberFoo32Ref, "x")
    val memberFoo32y = findParam(memberFoo32Ref, "y")
    val memberFoo32p1 = findParam(memberFoo32Ref, "p1")
    val memberFoo32p10 = findParam(memberFoo32Ref, "p10")
    val memberFoo32p32 = findParam(memberFoo32Ref, "p32")

    assertEquals(
        "a_b_42.0_10.0_32.0",
        memberFoo32Ref.callBy(
            mapOf(
                memberFoo32this to A(),
                memberFoo32x to "a",
                memberFoo32y to "b",
                memberFoo32p1 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_42.0_32.0",
        memberFoo32Ref.callBy(
            mapOf(
                memberFoo32this to A(),
                memberFoo32x to "a",
                memberFoo32y to "b",
                memberFoo32p10 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_10.0_42.0",
        memberFoo32Ref.callBy(
            mapOf(
                memberFoo32this to A(),
                memberFoo32x to "a",
                memberFoo32y to "b",
                memberFoo32p32 to 42.0
            )
        )
    )

    val memberFoo33Ref = findMember(A::class, "memberFoo33")
    val memberFoo33this = findInstanceParam(memberFoo33Ref)
    val memberFoo33x = findParam(memberFoo33Ref, "x")
    val memberFoo33y = findParam(memberFoo33Ref, "y")
    val memberFoo33p1 = findParam(memberFoo33Ref, "p1")
    val memberFoo33p10 = findParam(memberFoo33Ref, "p10")
    val memberFoo33p33 = findParam(memberFoo33Ref, "p33")

    assertEquals(
        "a_b_42.0_10.0_33.0",
        memberFoo33Ref.callBy(
            mapOf(
                memberFoo33this to A(),
                memberFoo33x to "a",
                memberFoo33y to "b",
                memberFoo33p1 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_42.0_33.0",
        memberFoo33Ref.callBy(
            mapOf(
                memberFoo33this to A(),
                memberFoo33x to "a",
                memberFoo33y to "b",
                memberFoo33p10 to 42.0
            )
        )
    )

    assertEquals(
        "a_b_1.0_10.0_42.0",
        memberFoo33Ref.callBy(
            mapOf(
                memberFoo33this to A(),
                memberFoo33x to "a",
                memberFoo33y to "b",
                memberFoo33p33 to 42.0
            )
        )
    )

    return "OK"
}