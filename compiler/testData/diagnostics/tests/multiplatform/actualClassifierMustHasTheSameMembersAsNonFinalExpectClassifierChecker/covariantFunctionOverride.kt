// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!> {
    open val foo: String = ""
    open fun foo(): Any = ""
}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> : Base {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING!>class Foo<!> : Base() {
    override fun foo(): <!RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING!>String<!> = ""
}
