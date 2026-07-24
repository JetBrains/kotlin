// LANGUAGE: +MultiPlatformProjects
// class: sample/Platform.Companion

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    companion object {
        val name: String
    }
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    <expr>actual companion object {
        actual val name: String = "JVM"
    }</expr>
}
