// MODULE: m1-common
// FILE: common.kt

open class Base {
    protected open fun foo() {}
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Foo : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    public override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}
