// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    <!ACTUAL_WITHOUT_EXPECT!>actual fun bar()<!>
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo {
    actual fun bar() {}
}
