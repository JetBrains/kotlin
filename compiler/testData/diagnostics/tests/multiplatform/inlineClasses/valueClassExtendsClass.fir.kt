// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class A(val s: String)

expect value class B(val s: String)

open class C

// MODULE: jvm()()(common)
// FILE: J.java
public class J {}

// FILE: jvm.kt
@JvmInline
actual value class A(val s: String) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>C<!>()

@JvmInline
actual value class B(val s: String) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>J<!>()
