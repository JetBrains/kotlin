// ISSUE: KT-68495
// WITH_STDLIB
// IGNORE_BACKEND: JVM, JVM_IR
// REASON: KT-68718 [JVM] Generic function is instantiated with wrong type argument

fun <T: Char> testContainsChar(arg: T): Boolean = arg in 'A'..'Z'

typealias CharAlias = Char
fun <T: X, X: CharAlias> testContainsNestedTypeArgs(arg: T): Boolean = arg in 'A'..'Z'

fun <T: X, X: Comparable<UInt>> testContainsUIntMultipleBounds(arg: T): Boolean
        where X: UInt
    = arg in 1U..10U

fun <T: X, X: UInt> testForUInt(arg: T): Int
        where X : Comparable<UInt> {
    var sum: Int = 0
    for (i in arg..arg+3U)
        sum += i.toInt()
    return sum
}

fun box(): String {
    if (!testContainsChar('T')) return "FAIL !testContainsChar('T')"
    if (testContainsChar('f')) return "FAIL testContainsChar('f')"
    if (!testContainsNestedTypeArgs('T')) return "FAIL !testContainsNestedTypeArgs('T')"
    if (testContainsNestedTypeArgs('f')) return "FAIL testContainsNestedTypeArgs('f')"
    if (!testContainsUIntMultipleBounds(3U)) return "FAIL !testContainsUIntMultipleBounds(3U)"
    if (testContainsUIntMultipleBounds(0U)) return "FAIL testContainsUIntMultipleBounds(0U)"
    if (testForUInt(0U) != 6) return "FAIL testForUInt(0U) != 6"
    return "OK"
}
