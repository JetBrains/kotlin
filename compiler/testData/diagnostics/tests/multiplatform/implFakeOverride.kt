// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class Foo {
    fun bar(): String
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

open class Bar {
    fun bar() = "bar"
}

impl class Foo : Bar()
