// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

open class Base {
    open val foo: Any = ""
    open fun foo(): String = ""
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open <!NON_FINAL_EXPECT_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_ACTUAL_CLASSIFIER!>class Foo<!> : Base() {
    override val foo: <!RETURN_TYPE_COVARIANT_OVERRIDE_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>String<!> = ""
}
