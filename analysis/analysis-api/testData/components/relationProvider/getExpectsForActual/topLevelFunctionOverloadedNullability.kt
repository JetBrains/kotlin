// LANGUAGE: +MultiPlatformProjects
// function: sample/some(b)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun some(a: Int): Int

expect fun some(b: Int?): Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual fun some(a: Int) = 42

<expr>actual fun some(b: Int?) = 42</expr>
