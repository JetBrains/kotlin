// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING

@JvmInline
value class IC(val x: UInt)

fun ic(x: IC) = x.x

@JvmInline
value class SimpleMFVC(val x: UInt, val y: IC, val z: String)

fun smfvc(ic: IC, x: SimpleMFVC, ic1: UInt) = ic(ic) + x.x + ic(x.y) + ic1

@JvmInline
value class GreaterMFVC(val x: SimpleMFVC, val y: IC, val z: SimpleMFVC)

fun gmfvc(ic: IC, x: GreaterMFVC, ic1: UInt) = smfvc(ic, x.x, 0U) + ic(x.y) + smfvc(IC(0U), x.z, ic1)

fun box(): String {
    val o1 = IC(2U)
    require(ic(o1) == 2U)
    val o2 = SimpleMFVC(1U, o1, "3")
    require(smfvc(IC(4U), o2, 5U) == 12U)
    val o3 = GreaterMFVC(o2, IC(6U), SimpleMFVC(7U, IC(8U), "9"))
    require(gmfvc(IC(10U), o3, 11U) == 45U)
    return "OK"
}