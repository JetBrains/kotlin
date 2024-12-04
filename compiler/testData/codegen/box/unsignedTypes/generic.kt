// ISSUE: KT-68718 [JVM] Generic function is instantiated with wrong type argument
// WITH_STDLIB
// IGNORE_BACKEND: JVM

fun <T: UInt> testUInt1(arg: T): Int = arg.toInt()

fun <T> T.noInlineApply(body: T.() -> Unit): T {
    body()
    return this
}

fun <T, R> T.noInlineRun(body: T.() -> R): R = body()

fun <T: UInt> testUInt2_1(old: T, value: T): Wrapper<T> = Wrapper(old).apply { this.value = value }
fun <T: UInt> testUInt2_2(old: T, value: T): Wrapper<T> = Wrapper(old).noInlineApply { this.value = value }
fun <T: UInt> testUInt3_1(old: T, value: T): Int = Wrapper(old).run { this.value = value; value.toInt() }
fun <T: UInt> testUInt3_2(old: T, value: T): Int = Wrapper(old).noInlineRun { this.value = value; value.toInt() }

public class Wrapper<T>(var value: T)


fun box(): String {
    if (testUInt1(42U) != 42) return "FAIL testUInt1(42U) != 42"
    
    if (testUInt2_1(42U, 43U).value != 43U) return "FAIL testUInt2(42U, 43U).value != 43U"
    if (testUInt2_2(42U, 43U).value != 43U) return "FAIL testUInt2(42U, 43U).value != 43U"
    
    if (testUInt3_1(42U, 43U) != 43) return "FAIL testUInt3(42U, 43U) != 43U"
    if (testUInt3_2(42U, 43U) != 43) return "FAIL testUInt3(42U, 43U) != 43U"
    
    return "OK"
}
