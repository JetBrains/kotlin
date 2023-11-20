// MODULE: m1-common
// FILE: common.kt
interface Base {
    fun foo() {}
}
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect abstract class Foo() : Base<!>


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual abstract class Foo : Base {
    abstract override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>()
}
