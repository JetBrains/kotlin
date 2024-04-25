// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect interface S1
expect interface S2

open <!CANNOT_INFER_VISIBILITY, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class A<!> : S1, S2

<!CANNOT_INFER_VISIBILITY!>class B<!> : A()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual interface S1 {
    fun f() {}
}

actual interface S2 {
    fun f() {}
}
