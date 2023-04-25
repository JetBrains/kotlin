// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect interface S1
expect interface S2

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED{JVM}!>open class A : S1, S2<!>

class B : A()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual interface S1 {
    fun f() {}
}

actual interface S2 {
    fun f() {}
}
