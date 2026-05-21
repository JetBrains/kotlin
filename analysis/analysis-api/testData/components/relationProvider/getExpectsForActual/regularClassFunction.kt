// LANGUAGE: +MultiPlatformProjects
// callable: sample/Platform.getName

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun getName(): String
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    <expr>actual fun getName(): String = "JVM"</expr>
}
