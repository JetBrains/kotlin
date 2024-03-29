// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    fun foo(p: Int = 1)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface Base {
    fun foo(p: Int)
}

object BaseImpl : Base {
    override fun foo(p: Int) {}
}

actual class Foo : <!DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE!>Base by BaseImpl<!>
