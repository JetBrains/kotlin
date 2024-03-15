// MODULE: m1-common
// FILE: common.kt

open class Base<R> {
    open fun foo(): R = null!!
}

expect open class Foo<R, T : R> : Base<R> {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<E, F : E> : Base<E>() {
    override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>(): F = null!!
}
