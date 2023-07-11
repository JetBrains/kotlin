// MODULE: m1-common
// FILE: common.kt

open class Base() {
    open fun overrideReturnType(): Any = ""
    open fun overrideModality1(): Any = ""
    open fun overrideModality2(): Any = ""
    protected open fun overrideVisibility(): Any = ""
}

expect open class Foo : Base {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    fun <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>injectedMethod<!>() {}
    val <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>injectedProperty<!>: Int = 42
    override fun overrideReturnType(): <!RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>String<!> = ""
    <!MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>final<!> override fun overrideModality1(): Any = ""
    <!MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>final<!> override fun overrideModality2(): Any = ""
    <!VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>public<!> override fun overrideVisibility(): Any = ""
}
