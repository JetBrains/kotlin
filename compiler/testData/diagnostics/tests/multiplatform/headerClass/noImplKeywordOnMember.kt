// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun bar(): String
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

actual class Foo {
    <!ACTUAL_MISSING!>fun bar(): String<!> = "bar"
}
