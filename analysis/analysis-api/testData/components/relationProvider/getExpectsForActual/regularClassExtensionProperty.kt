// LANGUAGE: +MultiPlatformProjects
// callable: sample/Platform.size

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    val String.size: Int
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    <expr>actual val String.size: Int
        get() = length</expr>
}
