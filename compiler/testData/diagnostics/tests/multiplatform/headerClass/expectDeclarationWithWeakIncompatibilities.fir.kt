// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect class Foo1
expect class Foo2

expect fun foo2(): Int

expect val s: String

expect open class Foo3

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

interface Foo1
actual interface <!ACTUAL_WITHOUT_EXPECT!>Foo2<!>

actual var <!ACTUAL_WITHOUT_EXPECT!>s<!>: String = "value"

fun foo2(): Int = 0

actual class <!ACTUAL_WITHOUT_EXPECT, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo3<!>

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo3<!>
