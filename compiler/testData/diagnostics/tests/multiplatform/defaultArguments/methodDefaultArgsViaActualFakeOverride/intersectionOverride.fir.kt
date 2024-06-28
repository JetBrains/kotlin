// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect interface Foo {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>fun foo(p: Int = 1)<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface Base1 {
    fun foo(p: Int)
}

interface Base2 {
    fun foo(p: Int)
}

@Suppress("ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING")
actual interface Foo : <!DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE!>Base1, Base2<!>
