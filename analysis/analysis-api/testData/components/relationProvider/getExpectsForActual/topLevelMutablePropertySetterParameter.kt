// LANGUAGE: +MultiPlatformProjects
// value_parameter: value: setter: callable: sample/name

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect var name: String

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual var name: String = "Alice"
    set(<expr>value</expr>) {}
