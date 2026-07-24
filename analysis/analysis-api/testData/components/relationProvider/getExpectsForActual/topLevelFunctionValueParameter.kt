// LANGUAGE: +MultiPlatformProjects
// value_parameter: n: callable: sample/some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun some(n: Int): Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual fun some(<expr>n: Int</expr>): Int = n
