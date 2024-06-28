// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo {
    fun bar(): String
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>fun bas(f: Int)<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo {
    fun <!ACTUAL_MISSING!>bar<!>(): String = "bar"
    fun <!ACTUAL_MISSING!>bas<!>(g: Int) {}
}
