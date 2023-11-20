// MODULE: m1-common
// FILE: common.kt

open class Base {
    open fun <T> foo(t: T) {}
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override fun <R> <!ACTUAL_WITHOUT_EXPECT!>foo<!>(t: R) {}
}
