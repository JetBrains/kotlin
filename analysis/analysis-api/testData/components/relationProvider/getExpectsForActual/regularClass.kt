// LANGUAGE: +MultiPlatformProjects
// class: sample/Platform

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    val name: String
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual class Platform {
    actual val name: String
        get() = "JVM"
}</expr>
