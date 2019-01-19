// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS, JVM_IR

const val M1: UInt = 2147483648u
const val M2: ULong = 9223372036854775808UL

fun testUInt(x: UInt) =
    when (x) {
        0u -> "none"
        1u -> "one"
        2u -> "two"
        3u -> "three"
        M1 -> "M1"
        else -> "many"
    }

fun testULong(x: ULong) =
    when (x) {
        0UL -> "none"
        1UL -> "one"
        2UL -> "two"
        3UL -> "three"
        M2 -> "M2"
        else -> "many"
    }

fun box(): String {
    val t1 = listOf(0u, 1u, 2u, 3u, 4u, M1).map { testUInt(it) }
    if (t1 != listOf("none", "one", "two", "three", "many", "M1")) throw AssertionError("UInt: $t1")

    val t2 = listOf(0UL, 1UL, 2UL, 3UL, 4UL, M2).map { testULong(it) }
    if (t2 != listOf("none", "one", "two", "three", "many", "M2")) throw AssertionError("ULong: $t2")

    return "OK"
}