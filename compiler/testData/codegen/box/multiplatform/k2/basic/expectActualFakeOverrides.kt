// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: commonMain.kt

expect class A() {
    fun foo(s: String): String

    val bar: String
}

fun test(s: String): String {
    val a = A()
    return a.foo(s) + a.bar
}

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

open class B {
    fun foo(s: String) = s

    val bar: String = "K"
}

actual class A : B()

fun box() = test("O")