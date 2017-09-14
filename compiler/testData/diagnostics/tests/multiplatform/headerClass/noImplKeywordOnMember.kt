// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class Foo {
    fun bar(): String
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl class Foo {
    <!IMPL_MISSING!>fun bar(): String<!> = "bar"
}
