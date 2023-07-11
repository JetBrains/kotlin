// MODULE: m1-common
// FILE: common.kt

open class Base {
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun foo(param: Int) {}<!>
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> : Base() {
    <!PARAMETER_NAME_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>override fun foo(paramNameChanged: Int) {}<!>
}
