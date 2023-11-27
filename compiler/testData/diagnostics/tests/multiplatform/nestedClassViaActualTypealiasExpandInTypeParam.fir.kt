// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>class Inner {
        <!EXPECT_ACTUAL_MISMATCH{JVM}!>fun foo(p: List<Inner>)<!>
    }<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Inner {
        fun foo(p: List<Inner>) {}
    }
}

actual typealias Foo = FooImpl
