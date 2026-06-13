// LANGUAGE: +MultiPlatformProjects
// class: sample/Color

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect enum class Color { RED, GREEN, BLUE }

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual enum class Color { RED, GREEN }</expr>
