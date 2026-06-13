// LANGUAGE: +MultiPlatformProjects
// callable: sample/some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun some(a: Int): Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// 'expect' and 'actual' parameters have different names
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

<expr>actual fun some(b: Int) = 42</expr>
