// LANGUAGE: +MultiPlatformProjects
// callable: sample/some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun <T : Any> some(): T

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// 'expect' and 'actual' type parameters have different names
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

<expr>actual fun <R : Any> some(): R = null!!</expr>
