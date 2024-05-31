// ISSUE: KT-68495
// REASON: compile-time failure:
//         java.lang.IllegalStateException
//         Has to be a class T of <root>.BoundedGenericValueInRangeCheckKt.test
//         @ org.jetbrains.kotlin.backend.common.lower.loops.UtilsKt.castIfNecessary(Utils.kt:187)
// WITH_STDLIB
// IGNORE_BACKEND: JVM, JVM_IR
// REASON: compile-time failure:
//         java.lang.IllegalArgumentException: T of <root>.BoundedGenericValueInRangeCheckKt.testUInt has to be a subtype of kotlin.Int
fun <T: Char> test(arg: T): Boolean = arg in 'A'..'Z'

typealias CharAlias = Char
fun <T: X, X: CharAlias> testNested(arg: T): Boolean = arg in 'A'..'Z'

fun <T: UInt> testUInt(arg: T): Int {
    var sum: Int = 0
    for (i in arg..arg+3U)
        sum += i.toInt()
    return sum
}

fun box(): String {
    if (!test('T')) return "FAIL !test('T')"
    if (test('f')) return "FAIL test('f')"
    if (!testNested('T')) return "FAIL !testNested('T')"
    if (testNested('f')) return "FAIL testNested('f')"
    if (testUInt(0U) != 6) return "FAIL testUInt(0U) != 6"
    return "OK"
}
