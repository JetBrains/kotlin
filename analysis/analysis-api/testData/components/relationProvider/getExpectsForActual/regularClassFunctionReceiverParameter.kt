// LANGUAGE: +MultiPlatformProjects
// callable: sample/Platform.foo

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun String.foo()
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    <expr>actual fun String.foo() {}</expr>
}
