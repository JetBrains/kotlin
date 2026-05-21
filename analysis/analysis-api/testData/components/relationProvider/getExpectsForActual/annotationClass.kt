// LANGUAGE: +MultiPlatformProjects
// class: sample/Marker

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect annotation class Marker

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual annotation class Marker</expr>
