// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Foo<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    final override fun <!ACTUAL_WITHOUT_EXPECT!>toString<!>() = "Foo"
}
