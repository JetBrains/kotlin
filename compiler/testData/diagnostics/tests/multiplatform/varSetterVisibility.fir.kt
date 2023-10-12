// MODULE: m1-common
// FILE: common.kt
<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect var v1: Boolean<!>

expect var v2: Boolean
    internal set

<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect var v3: Boolean
    internal set<!>

<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect open class C {
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>var foo: Boolean<!>
}<!>

<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect open class C2 {
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>var foo: Boolean<!>
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual var <!ACTUAL_WITHOUT_EXPECT!>v1<!>: Boolean = false
    private set

actual var v2: Boolean = false

actual var <!ACTUAL_WITHOUT_EXPECT!>v3<!>: Boolean = false
    private set

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_ERROR!>class C<!> {
    actual var <!ACTUAL_WITHOUT_EXPECT!>foo<!>: Boolean = false
        <!SETTER_VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_ERROR!>protected<!> set
}

open class C2Typealias {
    var foo: Boolean = false
        protected set
}

actual typealias <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_ERROR, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>C2<!> = C2Typealias
