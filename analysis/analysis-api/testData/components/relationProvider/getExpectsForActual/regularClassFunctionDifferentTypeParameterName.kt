// LANGUAGE: +MultiPlatformProjects
// callable: sample/Platform.some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun <T : Any> some(): T
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    <expr>actual fun <R : Any> some(): R = null!!</expr>
}
