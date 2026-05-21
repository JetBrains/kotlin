// LANGUAGE: +MultiPlatformProjects
// callable: sample/foo

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun String.foo()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual fun String.foo() {}</expr>
