// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

interface I
open class C
interface J

expect class Foo : I, C, J

expect class Bar : C<!SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS, JS:SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS, JVM:SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS!>()<!>

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
actual class Foo : I, C(), J

actual class <!ACTUAL_WITHOUT_EXPECT!>Bar<!>

// MODULE: m3-js(m1-common)
// FILE: js.kt
actual class Foo : I, J, C()

actual class Bar : C()
