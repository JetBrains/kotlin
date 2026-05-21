// LANGUAGE: +MultiPlatformProjects

// Ambiguity in '// callable' (overloads differ by receiver)
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun some(): Int
    fun String.some(): Int
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    actual fun some(): Int = 42

    <expr>actual fun String.some(): Int = length</expr>
}
