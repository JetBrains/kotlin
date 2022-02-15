// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun bar(): String
    fun bas(f: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo {
    fun <!ACTUAL_MISSING!>bar<!>(): String = "bar"
    fun <!ACTUAL_MISSING!>bas<!><!ACTUAL_WITHOUT_EXPECT!>(g: Int)<!> {}
}
