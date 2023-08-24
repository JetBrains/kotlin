// MODULE: m1-common
// FILE: common.kt

open class Base {
    open fun foo() {}
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> : Base() {
    <!MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>final<!> override fun foo() {}
}
