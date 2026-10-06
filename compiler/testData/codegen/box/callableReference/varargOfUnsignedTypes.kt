// WITH_STDLIB

fun uints(vararg xs: UInt): UInt = xs.sum()
fun ulongs(vararg xs: ULong): ULong = xs.sum()
fun ubytes(vararg xs: UByte): UInt = xs.sum()
fun ushorts(vararg xs: UShort): UInt = xs.sum()

fun <T, R> apply(f: (T) -> R, arg: T): R = f(arg)

fun spreadSmartCast(a: Any): UInt = if (a is UIntArray) uints(*a) else 0u

fun box(): String {
    if ((::uints)(uintArrayOf(1u, 2u)) != 3u) return "fail 1"
    if ((::ulongs)(ulongArrayOf(1uL, 2uL)) != 3uL) return "fail 2"
    if ((::ubytes)(ubyteArrayOf(1u, 2u)) != 3u) return "fail 3"
    if ((::ushorts)(ushortArrayOf(1u, 2u)) != 3u) return "fail 4"

    if (apply(::uints, uintArrayOf(1u, 2u)) != 3u) return "fail 5"
    if (apply(::ulongs, ulongArrayOf(1uL, 2uL)) != 3uL) return "fail 6"

    val ref: (UIntArray) -> UInt = ::uints
    val asAny: Any = ref
    @Suppress("UNCHECKED_CAST")
    if ((asAny as (UIntArray) -> UInt)(uintArrayOf(4u, 5u)) != 9u) return "fail 7"

    if (spreadSmartCast(uintArrayOf(1u, 2u)) != 3u) return "fail 8"

    return "OK"
}
