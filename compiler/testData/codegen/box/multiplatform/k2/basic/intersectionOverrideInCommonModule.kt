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

interface Base1 {
    fun foo(x: Expect): String
    val Expect.bar: String
}

interface Base2 {
    fun foo(x: Expect): String
    val Expect.bar: String
}

interface Derived : Base1, Base2

// MODULE: platform()()(common)
// FILE: platform.kt

class Impl : Derived {
    override fun foo(x: Expect): String = x.o()
    override val Expect.bar: String get() = this.k
}

class ActualTarget {
    fun o(): String = "O"
    val k: String = "K"
}

actual typealias Expect = ActualTarget

fun test(x: Derived): String {
    val actual = ActualTarget()
    return x.foo(actual) + with(x) { actual.k }
}

fun box(): String {
    return test(Impl())
}
