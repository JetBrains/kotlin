// MODULE: m1-common
// FILE: common.kt

open class Base {
    open val foo: Int = 1
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override var <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>foo<!>: Int = 1
}
