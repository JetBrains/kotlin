// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>class Bar() {
        <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>fun foo(p: Int = 1)<!>
    }<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
open class Base {
    fun foo(p: Int) {}
}

actual class Foo {
    actual class Bar : <!DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE!>Base()<!>
}
