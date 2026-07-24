// WITH_STDLIB

// LANGUAGE: +MultiPlatformProjects
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun foo(text: String)

// MODULE: js()()(common)
// TARGET_PLATFORM: JS
// FILE: JavaScript.kt

package sample

actual fun foo(text: Str<caret>ing) {}
