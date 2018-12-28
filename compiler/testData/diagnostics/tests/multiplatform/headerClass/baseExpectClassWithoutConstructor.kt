// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect open class A
expect class B : A
open class C : <!JVM:SUPERTYPE_NOT_INITIALIZED, SUPERTYPE_NOT_INITIALIZED!>A<!>

// MODULE: m1-jvm(m1-common)
// FILE: jvm.kt

actual open class A
actual class B : A()