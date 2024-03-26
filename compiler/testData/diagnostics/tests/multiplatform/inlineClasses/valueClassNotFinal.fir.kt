// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect final value class A(val s: String)

// MODULE: jvm()()(common)
// FILE: jvm.kt
@JvmInline
actual <!VALUE_CLASS_NOT_FINAL!>open<!> value class A(val s: String)

class B : A("")
