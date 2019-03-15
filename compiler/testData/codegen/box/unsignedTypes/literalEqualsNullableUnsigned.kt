// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

fun isZeroUInt(n: UInt?) = 0U == n
fun isZeroULong(n: ULong?) = 0UL == n

fun box(): String {
    if (isZeroUInt(null)) throw AssertionError()
    if (isZeroUInt(1U)) throw AssertionError()
    if (!isZeroUInt(0U)) throw AssertionError()

    if (isZeroULong(null)) throw AssertionError()
    if (isZeroULong(1UL)) throw AssertionError()
    if (!isZeroULong(0UL)) throw AssertionError()

    return "OK"
}