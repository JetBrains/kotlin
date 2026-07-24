// LANGUAGE: +MultiPlatformProjects
// getter: callable: sample/userName

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect var userName: String

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual var userName: String
    <expr>get() = "Alice"</expr>
    set(value) {}
