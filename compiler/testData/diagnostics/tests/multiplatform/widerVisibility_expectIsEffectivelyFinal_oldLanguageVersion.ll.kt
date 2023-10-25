// LANGUAGE: -SupportEffectivelyFinalInExpectActualVisibilityCheck
// MODULE: m1-common
// FILE: common.kt

open class Base {
    internal open fun foo() {}
}
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo : Base {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>override fun foo()<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : Base() {
    public actual override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {
    }
}
