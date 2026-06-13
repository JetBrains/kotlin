// LANGUAGE: +MultiPlatformProjects
// type_parameter: T: callable: sample/some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun <T : Any> some(): T

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual fun <<expr>T : Any</expr>> some(): T = null!!
