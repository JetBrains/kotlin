// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect class Foo1
expect class Foo2

expect fun foo2(): Int

expect val s: String

expect open class Foo3

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

interface <!ACTUAL_MISSING!>Foo1<!>
actual interface <!EXPECT_ACTUAL_INCOMPATIBILITY_CLASS_KIND!>Foo2<!>

actual var <!EXPECT_ACTUAL_INCOMPATIBILITY_PROPERTY_KIND!>s<!>: String = "value"

fun <!ACTUAL_MISSING!>foo2<!>(): Int = 0

actual class <!CLASSIFIER_REDECLARATION, EXPECT_ACTUAL_INCOMPATIBILITY_MODALITY!>Foo3<!>

class <!ACTUAL_MISSING, CLASSIFIER_REDECLARATION!>Foo3<!>
