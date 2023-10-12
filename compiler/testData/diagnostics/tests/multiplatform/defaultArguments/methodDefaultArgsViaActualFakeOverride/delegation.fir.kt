// MODULE: m1-common
// FILE: common.kt
<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect class Foo {
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>fun foo(param: Int = 1)<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface Base {
    fun foo(p: Int)
}

object BaseImpl : Base {
    override fun foo(p: Int) {}
}

// todo broken test
actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> : Base by BaseImpl
