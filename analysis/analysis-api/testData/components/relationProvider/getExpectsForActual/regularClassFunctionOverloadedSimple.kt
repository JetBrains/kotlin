// LANGUAGE: +MultiPlatformProjects

// Ambiguity in '// callable' (multiple overloads of the same function)
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun compute(n: Int): Int
    fun compute(text: String): Int
    fun unrelated(n: Int): Int
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    <expr>actual fun compute(n: Int): Int = n</expr>
    actual fun compute(text: String): Int = text.length
    actual fun unrelated(n: Int): Int = 42
}
