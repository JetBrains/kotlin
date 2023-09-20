// MODULE: m1-common
// FILE: common.kt
interface Base {
    <!INCOMPATIBLE_MATCHING{JVM}!>fun foo()<!>
}
<!INCOMPATIBLE_MATCHING{JVM}!>expect open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Foo<!>() : Base<!>


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

// Mismatched scope must be reported here. But it's false negative checker in K1.
// For some reason, K1 says that modality of `exect_Foo.foo` is `abstract`.
// https://youtrack.jetbrains.com/issue/KT-59739
actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> : Base {
    override fun <!MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>foo<!>() {}
}
