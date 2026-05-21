// WITH_STDLIB

// LANGUAGE: +MultiPlatformProjects
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun foo(text: String)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual fun foo(text: Str<caret>ing) {}
