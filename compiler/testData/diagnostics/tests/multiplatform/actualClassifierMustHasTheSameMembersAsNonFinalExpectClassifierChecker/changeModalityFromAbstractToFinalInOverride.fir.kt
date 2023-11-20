// MODULE: m1-common
// FILE: common.kt

interface Base {
    fun foo()
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Foo<!> : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base {
    final override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}
