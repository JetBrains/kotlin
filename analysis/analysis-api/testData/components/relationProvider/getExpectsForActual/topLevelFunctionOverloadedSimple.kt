// LANGUAGE: +MultiPlatformProjects
// function: sample/some()

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun some(): Int
expect fun some(param: String): String
expect fun unrelated(): Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual fun some(): Int = 42</expr>
actual fun some(param: String): String = param
actual fun unrelated(): Int = 10
