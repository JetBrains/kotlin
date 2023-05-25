// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect open class C1()
expect interface I1

open class A : C1(), I1
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED{JVM}!>open class B : I1, C1()<!>

expect abstract class C2()
expect interface I2

// TODO: KT-58829
class C : C2(), I2

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual open class C1 {
    fun f() {}
}

actual interface I1 {
    fun f() {}
}

actual abstract class C2 actual constructor() {
    fun g() {}
}

actual interface I2 {
    fun g()
}
