// WITH_REFLECT
// TARGET_BACKEND: JVM

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.reflect.full.primaryConstructor

@JvmInline
value class Id(val value: String)

class Holder @JvmExposeBoxed constructor(
    val p00: Id = Id("00"),
    val p01: Id = Id("01"),
    val p02: Id = Id("02"),
    val p03: Id = Id("03"),
    val p04: Id = Id("04"),
    val p05: Id = Id("05"),
    val p06: Id = Id("06"),
    val p07: Id = Id("07"),
    val p08: Id = Id("08"),
    val p09: Id = Id("09"),
    val p10: Id = Id("10"),
    val p11: Id = Id("11"),
    val p12: Id = Id("12"),
    val p13: Id = Id("13"),
    val p14: Id = Id("14"),
    val p15: Id = Id("15"),
    val p16: Id = Id("16"),
    val p17: Id = Id("17"),
    val p18: Id = Id("18"),
    val p19: Id = Id("19"),
    val p20: Id = Id("20"),
    val p21: Id = Id("21"),
    val p22: Id = Id("22"),
    val p23: Id = Id("23"),
    val p24: Id = Id("24"),
    val p25: Id = Id("25"),
    val p26: Id = Id("26"),
    val p27: Id = Id("27"),
    val p28: Id = Id("28"),
    val p29: Id = Id("29"),
    val p30: Id = Id("30"),
    val p31: Id = Id("OK"),
)

fun box(): String {
    return Holder::class.primaryConstructor!!.callBy(emptyMap()).p31.value
}
