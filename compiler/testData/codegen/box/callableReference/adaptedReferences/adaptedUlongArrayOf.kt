// WITH_STDLIB
// ISSUE: KT-89920
@file:OptIn(ExperimentalUnsignedTypes::class)

fun box(): String {
    val adapted: (ULong, ULong) -> ULongArray = ::ulongArrayOf
    val a = adapted(1uL, 2uL)
    if (!a.contentEquals(ulongArrayOf(1uL, 2uL))) return "Fail adapted: ${a.contentToString()}"

    val lambda: (ULong, ULong) -> ULongArray = { x, y -> ulongArrayOf(x, y) }
    val b = lambda(1uL, 2uL)
    if (!b.contentEquals(ulongArrayOf(1uL, 2uL))) return "Fail lambda: ${b.contentToString()}"

    val adaptedUInt: (UInt, UInt) -> UIntArray = ::uintArrayOf
    val c = adaptedUInt(1u, 2u)
    if (!c.contentEquals(uintArrayOf(1u, 2u))) return "Fail adapted uint: ${c.contentToString()}"

    return "OK"
}
