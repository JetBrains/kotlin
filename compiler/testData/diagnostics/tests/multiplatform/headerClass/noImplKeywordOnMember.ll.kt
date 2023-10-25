// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo {
    fun bar(): String
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>fun bas(f: Int)<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> {
    fun bar(): String = "bar"
    fun bas(g: Int) {}
}
