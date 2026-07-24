// LANGUAGE: +MultiPlatformProjects
// callable: sample/Platform.some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun some(a: Int): Int
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// 'expect' and 'actual' parameters have different names
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

actual class Platform {
    <expr>actual fun some(b: Int) = 42</expr>
}
