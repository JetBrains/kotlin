// LANGUAGE: +MultiPlatformProjects
// context_parameter: text: callable: sample/Platform.foo

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    context(text: String)
    fun foo()
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    context(<expr>text: String</expr>)
    actual fun foo() {}
}
