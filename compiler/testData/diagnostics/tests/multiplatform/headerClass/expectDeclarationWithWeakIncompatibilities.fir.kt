// DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo1<!>
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo2<!>

expect fun foo2(): Int

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect val s: String<!>

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Foo3<!>

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

interface <!ACTUAL_MISSING!>Foo1<!>
actual interface <!ACTUAL_WITHOUT_EXPECT!>Foo2<!>

actual var <!ACTUAL_WITHOUT_EXPECT!>s<!>: String = "value"

fun <!ACTUAL_MISSING!>foo2<!>(): Int = 0

actual class <!ACTUAL_WITHOUT_EXPECT, CLASSIFIER_REDECLARATION!>Foo3<!>

class <!ACTUAL_MISSING, CLASSIFIER_REDECLARATION!>Foo3<!>
