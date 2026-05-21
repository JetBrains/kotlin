// LANGUAGE: +MultiPlatformProjects

// Ambiguity in '// callable' (overloads differ by context parameters)
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun some(): Int

    context(text: String)
    fun some(): Int
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    actual fun some(): Int = 42

    <expr>context(text: String)
    actual fun some(): Int {
        return text.length
    }</expr>
}
