// LANGUAGE: +MultiPlatformProjects
// class: sample/Box

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Box<T>

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual class Box<T, R></expr>
