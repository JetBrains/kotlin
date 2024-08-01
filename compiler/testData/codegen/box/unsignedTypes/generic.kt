// ISSUE: KT-68718 [JVM] Generic function is instantiated with wrong type argument
// WITH_STDLIB
// IGNORE_BACKEND: JVM

fun <T: UInt> testUInt(arg: T): Int = arg.toInt()

fun box(): String {
    if (testUInt(42U) != 42) return "FAIL testUInt(42U) != 42"
    return "OK"
}
