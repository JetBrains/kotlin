// WITH_STDLIB
// DUMP_IR

fun uint(vararg us: UInt): UIntArray = us

fun nullableUInt(vararg us: UInt?): UIntArray {
    val ls = us.filterNotNull()
    return UIntArray(ls.size) { ls[it] }
}

inline fun inlinedUInt(vararg us: UInt): UIntArray = us

annotation class AnnoUByte(vararg val args: UByte)
@AnnoUByte()
val ubyte0 = 0
@AnnoUByte(0U)
val ubyte1 = 1

annotation class AnnoUShort(vararg val args: UShort)
@AnnoUShort()
val ushort0 = 0
@AnnoUShort(0U)
val ushort1 = 1

annotation class AnnoUInt(vararg val args: UInt)
@AnnoUInt()
val uint0 = 0
@AnnoUInt(0U)
val uint1 = 1

annotation class AnnoULong(vararg val args: ULong)
@AnnoULong()
val ulong0 = 0
@AnnoULong(0U)
val ulong1 = 1

fun box(): String {
    val uints = uint(1u, 2u, 3u)
    if (sum(*uints) != 6u) return "Fail 1"

    val complextUInts = uint(4u, *uints, 5u, *uints, 6u)
    if (sum(*complextUInts) != 27u) return "Fail 2"

    val nullableUInts = nullableUInt(1u, null, 2u, null)
    if (sum(*nullableUInts) != 3u) return "Fail 3"

    val inlinedUInts = inlinedUInt(1u, 3u)
    if (sum(*inlinedUInts) != 4u) return "Fail 4"

    val complexInlinedUInts = inlinedUInt(*inlinedUInts, 3u, *inlinedUInts)
    if (sum(*complexInlinedUInts) != 11u) return "Fail 5"

    if (nullableUInts !is UIntArray) return "Fail 6"

    if (inlinedUInts !is UIntArray) return "Fail 7"

    return "OK"
}

fun sum(vararg us: UInt): UInt {
    var sum = 0u
    for (i in us) {
        sum += i
    }

    return sum
}