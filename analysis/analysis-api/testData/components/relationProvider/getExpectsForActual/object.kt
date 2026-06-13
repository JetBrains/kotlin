// LANGUAGE: +MultiPlatformProjects
// class: sample/Platform

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect object Platform {
    val name: String
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual object Platform {
    actual val name: String = "JVM"
}</expr>
