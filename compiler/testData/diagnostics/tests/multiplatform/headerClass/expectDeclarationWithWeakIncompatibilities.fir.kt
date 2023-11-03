// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo1<!>
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo2<!>

expect fun foo2(): Int

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect val s: String<!>

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Foo3<!>

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

interface Foo1
actual interface <!ACTUAL_WITHOUT_EXPECT!>Foo2<!>

actual var <!ACTUAL_WITHOUT_EXPECT!>s<!>: String = "value"

fun foo2(): Int = 0

actual class <!ACTUAL_WITHOUT_EXPECT, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo3<!>

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo3<!>
