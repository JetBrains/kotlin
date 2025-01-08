// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

interface I
open class C
interface J

expect class Foo : I, C, J

expect class Bar : <!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR, SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR{JVM}!>C<!SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS, SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS{JVM}!>()<!><!>

expect class WithExplicitPrimaryConstructor() : C<!SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS, SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS{JVM}!>()<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo : I, C(), J

actual class <!ACTUAL_WITHOUT_EXPECT!>Bar<!>

actual class WithExplicitPrimaryConstructor : C()
