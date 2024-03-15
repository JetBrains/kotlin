// MODULE: m1-common
// FILE: common.kt
interface Base {
    fun foo() {}
}
expect abstract class Foo() : Base


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual abstract class Foo : Base {
    abstract override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>()
}
