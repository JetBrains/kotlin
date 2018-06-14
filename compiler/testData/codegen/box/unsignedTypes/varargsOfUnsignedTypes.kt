// !LANGUAGE: +InlineClasses
// !SKIP_METADATA_VERSION_CHECK
// WITH_UNSIGNED

fun uint(vararg us: UInt): UIntArray = us

fun nullableUInt(vararg us: UInt?): UIntArray {
    val ls = us.filterNotNull()
    return UIntArray(ls.size) { ls[it] }
}

inline fun inlinedUInt(vararg us: UInt): UIntArray = us

fun box(): String {
    val uints = uint(1u, 2u, 3u)
    if (sum(*uints) != 6u) return "Fail 1"

    val nullableUInts = nullableUInt(1u, null, 2u, null)
    if (sum(*nullableUInts) != 3u) return "Fail 2"

    val inlinedUInts = inlinedUInt(1u, 3u)
    if (sum(*inlinedUInts) != 4u) return "Fail 3"

    return "OK"
}

fun sum(vararg us: UInt): UInt {
    var sum = 0u
    for (i in us) {
        sum += i
    }

    return sum
}