// LANGUAGE: +MultiPlatformProjects
// callable: sample/foo

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun foo()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// The 'actual' function has an incompatible visibility (internal)
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

<expr>internal actual fun foo() {}</expr>
