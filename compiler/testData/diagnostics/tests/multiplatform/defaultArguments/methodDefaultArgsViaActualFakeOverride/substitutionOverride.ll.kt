// MODULE: m1-common
// FILE: common.kt
expect class Foo() {
    fun foo(p: Int = 1)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
open class Base<T> {
    fun foo(p: T) {}
}

actual class Foo : <!DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE!>Base<Int>()<!>
