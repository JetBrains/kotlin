// MODULE: m1-common
// FILE: common.kt

interface I
open class C
interface J

expect class Foo : I, C, J

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR, SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>expect class Bar : C<!SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS, SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS!>()<!><!>

expect class WithExplicitPrimaryConstructor() : C<!SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS, SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS!>()<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo : I, C(), J

actual class <!ACTUAL_WITHOUT_EXPECT!>Bar<!>

actual class WithExplicitPrimaryConstructor : C()

// MODULE: m3-js()()(m1-common)
// FILE: js.kt
actual class Foo : I, J, C()

actual class Bar : C()

actual class WithExplicitPrimaryConstructor : C()
