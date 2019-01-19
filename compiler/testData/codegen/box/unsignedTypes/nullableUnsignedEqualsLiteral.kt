// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

fun isZeroUInt(n: UInt?) = n == 0U
fun isZeroUInt2(n: UInt?): Boolean = n != null && n == 0u
fun isZeroULong(n: ULong?) = n == 0UL

fun box(): String {
    if (isZeroUInt(null)) throw AssertionError()
    if (isZeroUInt(1U)) throw AssertionError()
    if (!isZeroUInt(0U)) throw AssertionError()

    if (isZeroUInt2(null)) throw AssertionError()
    if (isZeroUInt2(1U)) throw AssertionError()
    if (!isZeroUInt2(0U)) throw AssertionError()

    if (isZeroULong(null)) throw AssertionError()
    if (isZeroULong(1UL)) throw AssertionError()
    if (!isZeroULong(0UL)) throw AssertionError()

    return "OK"
}