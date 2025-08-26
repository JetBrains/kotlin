// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

annotation class AnnBool(val xs: BooleanArray)
annotation class AnnUnsigned(
    val ub: UByteArray,
    val us: UShortArray,
    val ui: UIntArray,
    val ul: ULongArray
)

fun box(): String {
    val b1 = AnnBool(booleanArrayOf(true, false, true))
    val b2 = AnnBool(booleanArrayOf(true, false, true))
    val b3 = AnnBool(booleanArrayOf(true, true, false))
    if (b1 != b2) return "Fail1"
    if (b1.hashCode() != b2.hashCode()) return "Fail2"
    if (b1 == b3) return "Fail3"

    val u1 = AnnUnsigned(
        ub = ubyteArrayOf(1u, 2u),
        us = ushortArrayOf(1u, 2u),
        ui = uintArrayOf(1u, 2u),
        ul = ulongArrayOf(1u, 2u)
    )
    val u2 = AnnUnsigned(
        ub = ubyteArrayOf(1u, 2u),
        us = ushortArrayOf(1u, 2u),
        ui = uintArrayOf(1u, 2u),
        ul = ulongArrayOf(1u, 2u)
    )
    val u3 = AnnUnsigned(
        ub = ubyteArrayOf(2u, 1u),
        us = ushortArrayOf(2u, 1u),
        ui = uintArrayOf(2u, 1u),
        ul = ulongArrayOf(2u, 1u)
    )

    if (u1 != u2) return "Fail4"
    if (u1.hashCode() != u2.hashCode()) return "Fail5"
    if (u1 == u3) return "Fail6"

    return "OK"
}