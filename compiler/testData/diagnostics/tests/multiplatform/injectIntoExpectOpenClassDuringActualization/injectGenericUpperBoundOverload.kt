// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

open class Base {
    open fun <T> foo(t: T) {}
}

expect class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : Base() {
    fun <T : Comparable<T>> <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>foo<!>(t: T) {}
}
