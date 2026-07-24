// LANGUAGE: +MultiPlatformProjects
// callable: sample/some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun <T> some(): T

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual fun <T, R> some(): T = null!!</expr>
