// LANGUAGE: +MultiPlatformProjects

// Context parameters are not addressable by 'TestSymbolTarget'
// DISABLE_COORDINATE_TEST

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
