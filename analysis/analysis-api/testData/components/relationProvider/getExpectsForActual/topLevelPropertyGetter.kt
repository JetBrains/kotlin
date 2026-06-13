// LANGUAGE: +MultiPlatformProjects
// getter: callable: sample/userName

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect val userName: String

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual val userName: String
    <expr>get() = "Alice"</expr>
