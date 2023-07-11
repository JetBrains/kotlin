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

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!><T : I> : Base() {
    override fun foo(): <!RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>T<!> = null!!
}
