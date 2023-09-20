// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun <T> foo(t: T)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> {
    actual fun <T> foo(t: T) {}
    fun <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>foo<!>(t: String) {}
}
