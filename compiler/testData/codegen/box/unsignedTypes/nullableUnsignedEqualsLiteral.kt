// WITH_UNSIGNED
// IGNORE_BACKEND: JVM_IR

fun isZeroUInt(n: UInt?) = n == 0U
fun isZeroULong(n: ULong?) = n == 0UL

fun box(): String {
    if (isZeroUInt(null)) throw AssertionError()
    if (isZeroUInt(1U)) throw AssertionError()
    if (!isZeroUInt(0U)) throw AssertionError()

    if (isZeroULong(null)) throw AssertionError()
    if (isZeroULong(1UL)) throw AssertionError()
    if (!isZeroULong(0UL)) throw AssertionError()

    return "OK"
}