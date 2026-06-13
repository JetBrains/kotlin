// LANGUAGE: +MultiPlatformProjects
// context_parameter: text: callable: sample/foo

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

context(text: String)
expect fun foo()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

context(<expr>text: String</expr>)
actual fun foo() {}
