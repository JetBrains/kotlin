// ISSUE: KT-68801
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
open expect class A() {
    fun foo(): String
}

expect class B() : A

fun test() = B().foo()

// MODULE: platform()()(common)
// FILE: platform.kt
open class Base {
    fun foo() = "OK"
}

actual open class A : Base()

actual class B : A()

fun box() : String {
    return test()
}