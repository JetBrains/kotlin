// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-58229
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect class Expect {
    fun o(): String
    val k: String
}

interface Base<E> {
    fun foo(x: Expect): String = x.o()
    val Expect.bar: String get() = this.k
}

interface Derived : Base<Any?>

fun testCommon(expect: Expect): String {
    class LocalCommon : Derived
    val x = LocalCommon()

    return x.foo(expect) + with(x) { expect.k }
}

// MODULE: platform()()(common)
// FILE: platform.kt

class ActualTarget {
    fun o(): String = "O"
    val k: String = "K"
}

actual typealias Expect = ActualTarget

fun testPlatform(actual: ActualTarget): String {
    class LocalPlatform : Derived
    val x = LocalPlatform()

    return x.foo(actual) + with(x) { actual.k }
}

fun box(): String {
    val actual = ActualTarget()
    if (testCommon(actual) != "OK") return "Fail"
    return testPlatform(actual)
}
