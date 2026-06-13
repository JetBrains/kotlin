// LANGUAGE: +MultiPlatformProjects
// class: sample/Box

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect interface Box {
    val value: String
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual interface Box {
    actual val value: String
}</expr>
