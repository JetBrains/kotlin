// MODULE: m1-common
// FILE: common.kt

interface I

open class Base {
    open fun foo(): I = null!!
}

expect open class Foo<T : I> : Base {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<T : I> : Base() {
    override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>(): T = null!!
}
