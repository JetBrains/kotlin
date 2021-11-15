// WITH_STDLIB

fun isZeroUInt(n: UInt?) = n!! == 0U
fun isZeroUInt2(n: UInt?) = 0U == n!!
fun isZeroULong(n: ULong?) = n!! == 0UL
fun isZeroULong2(n: ULong?) = 0UL == n!!

fun isNullUInt(n: UInt?) = n == null
fun isNullUInt2(n: UInt?) = null == n
fun isNullULong(n: ULong?) = n == null
fun isNullULong2(n: ULong?) = null == n

fun box(): String {
    if (isZeroUInt(1U)) throw AssertionError()
    if (isZeroUInt2(1U)) throw AssertionError()
    if (!isZeroUInt(0U)) throw AssertionError()
    if (!isZeroUInt2(0U)) throw AssertionError()

    if (isZeroULong(1UL)) throw AssertionError()
    if (isZeroULong2(1UL)) throw AssertionError()
    if (!isZeroULong(0UL)) throw AssertionError()
    if (!isZeroULong2(0UL)) throw AssertionError()

    if (isNullUInt(1U)) throw AssertionError()
    if (isNullUInt2(1U)) throw AssertionError()
    if (!isNullUInt(null)) throw AssertionError()
    if (!isNullUInt2(null)) throw AssertionError()

    if (isNullULong(1UL)) throw AssertionError()
    if (isNullULong2(1UL)) throw AssertionError()
    if (!isNullULong(null)) throw AssertionError()
    if (!isNullULong2(null)) throw AssertionError()

    return "OK"
}