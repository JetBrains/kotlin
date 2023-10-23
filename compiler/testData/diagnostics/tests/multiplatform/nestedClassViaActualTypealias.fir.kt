// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>class Inner<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>()<!><!>
}<!>

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class SeveralInner {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>class Inner1 {
        <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>class Inner2 {
            <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>class Inner3<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>()<!><!>
        }<!>
    }<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Inner
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = FooImpl

class SeveralInnerImpl {
    class Inner1 {
        class Inner2 {
            class Inner3
        }
    }
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>SeveralInner<!> = SeveralInnerImpl
