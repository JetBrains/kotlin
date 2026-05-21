// LANGUAGE: +MultiPlatformProjects
// function: sample/Platform.some(b)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun some(a: Int): Int

    fun some(b: Int?): Int
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    actual fun some(a: Int) = 42

    <expr>actual fun some(b: Int?) = 42</expr>
}
