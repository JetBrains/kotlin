// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

class Foo {
    expect fun bar(): String
}

// MODULE: m1-jvm(m1-common)
// FILE: jvm.kt
