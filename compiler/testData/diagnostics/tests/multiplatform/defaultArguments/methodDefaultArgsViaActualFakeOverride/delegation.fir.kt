// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>fun foo(param: Int = 1)<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface Base {
    fun foo(p: Int)
}

object BaseImpl : Base {
    override fun foo(p: Int) {}
}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> : Base by BaseImpl
