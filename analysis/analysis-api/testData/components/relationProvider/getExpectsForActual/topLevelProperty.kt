// LANGUAGE: +MultiPlatformProjects
// callable: sample/userName
// MODULE_SQUAD: kmp(common,jvm)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect val userName: String

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual val userName: String
    get() = "Alice"</expr>
