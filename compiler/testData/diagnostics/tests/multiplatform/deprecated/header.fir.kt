// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class My

header fun foo(): Int

header val x: String

header object O

header enum class E {
    FIRST
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

impl class My

impl fun foo() = 42

impl val x get() = "Hello"

impl object O

impl enum class E {
    FIRST
}
