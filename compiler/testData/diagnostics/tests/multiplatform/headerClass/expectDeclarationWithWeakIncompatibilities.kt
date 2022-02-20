// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect class Foo1
expect class Foo2

expect fun foo2(): Int

expect val s: String

expect open class <!AMBIGUOUS_ACTUALS{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>Foo3<!>

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

interface <!ACTUAL_MISSING!>Foo1<!>
actual <!ACTUAL_WITHOUT_EXPECT!>interface<!> Foo2

actual <!ACTUAL_WITHOUT_EXPECT!>var<!> s: String = "value"

fun <!ACTUAL_MISSING!>foo2<!>(): Int = 0

actual class <!ACTUAL_WITHOUT_EXPECT, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo3<!>

class <!ACTUAL_MISSING, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo3<!>
