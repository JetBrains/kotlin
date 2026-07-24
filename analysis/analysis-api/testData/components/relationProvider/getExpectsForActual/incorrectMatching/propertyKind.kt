// LANGUAGE: +MultiPlatformProjects
// callable: sample/name

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect val name: String

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual var name: String = "Alice"</expr>
