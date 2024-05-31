// ISSUE: KT-68495
// REASON: compile-time failure:
//         java.lang.IllegalStateException
//         Has to be a class T of <root>.BoundedGenericValueInRangeCheckKt.test
//         @ org.jetbrains.kotlin.backend.common.lower.loops.UtilsKt.castIfNecessary(Utils.kt:187)

fun <T: Char> test(arg: T): Boolean = arg in 'A'..'Z'

typealias CharAlias = Char
fun <T: X, X: CharAlias> testNested(arg: T): Boolean = arg in 'A'..'Z'

fun box(): String {
    if (!test('T')) return "FAIL !test('T')"
    if (test('f')) return "FAIL test('f')"
    if (!testNested('T')) return "FAIL !testNested('T')"
    if (testNested('f')) return "FAIL testNested('f')"
    return "OK"
}
