// LANGUAGE: +MultiPlatformProjects
// callable: sample/Platform.name

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

actual class Platform {
    <expr>actual val name: String
        get() = "JVM"</expr>
}
