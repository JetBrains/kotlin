// TARGET_BACKEND: JVM

// LANGUAGE: +ContextParameters
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.test.assertEquals

fun String.topLevel30(
    p1: Double = 1.0, p2: Double = 2.0, p3: Double = 3.0, p4: Double = 4.0, p5: Double = 5.0, p6: Double = 6.0,
    p7: Double = 7.0, p8: Double = 8.0, p9: Double = 9.0, p10: Double = 10.0, p11: Double = 11.0, p12: Double = 12.0,
    p13: Double = 13.0, p14: Double = 14.0, p15: Double = 15.0, p16: Double = 16.0, p17: Double = 17.0, p18: Double = 18.0,
    p19: Double = 19.0, p20: Double = 20.0, p21: Double = 21.0, p22: Double = 22.0, p23: Double = 23.0, p24: Double = 24.0,
    p25: Double = 25.0, p26: Double = 26.0, p27: Double = 27.0, p28: Double = 28.0, p29: Double = 29.0, p30: Double = 30.0,
) = "${this}_${p1}_${p10}_${p30}"

fun String.topLevel31(
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
    p31: Double = 30.0,
) = "${this}_${p1}_${p10}_${p31}"

fun String.topLevel32(
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
) = "${this}_${p1}_${p10}_${p32}"

fun String.topLevel33(
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
) = "${this}_${p1}_${p10}_${p33}"

fun findExtensionParam(callable: KCallable<*>): KParameter =
    callable.parameters.single { it.kind == KParameter.Kind.EXTENSION_RECEIVER }

fun findParam(callable: KCallable<*>, name: String): KParameter = callable.parameters.single { it.name == name }

fun box(): String {

    var ref: KCallable<*> = String::topLevel30

    assertEquals(
        "a".topLevel30(p1 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p1") to 42.0
            )
        )
    )

    assertEquals(
        "a".topLevel30(p10 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p10") to 42.0
            )
        )
    )

    assertEquals(
        "a".topLevel30(p30 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p30") to 42.0
            )
        )
    )

    ref = String::topLevel31

    assertEquals(
        "a".topLevel31(p1 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p1") to 42.0
            )
        )
    )

    assertEquals(
        "a".topLevel31(p10 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p10") to 42.0
            )
        )
    )

    assertEquals(
        "a".topLevel31(p31 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p31") to 42.0
            )
        )
    )

    ref = String::topLevel32

    assertEquals(
        "a".topLevel32(p1 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p1") to 42.0
            )
        )
    )

    assertEquals(
        "a".topLevel32(p10 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p10") to 42.0
            )
        )
    )

    assertEquals(
        "a".topLevel32(p32 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p32") to 42.0
            )
        )
    )

    ref = String::topLevel33

    assertEquals(
        "a".topLevel33(p1 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p1") to 42.0
            )
        )
    )

    assertEquals(
        "a".topLevel33(p10 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p10") to 42.0
            )
        )
    )

    assertEquals(
        "a".topLevel33(p33 = 42.0),
        ref.callBy(
            mapOf(
                findExtensionParam(ref) to "a",
                findParam(ref, "p33") to 42.0
            )
        )
    )

    return "OK"
}