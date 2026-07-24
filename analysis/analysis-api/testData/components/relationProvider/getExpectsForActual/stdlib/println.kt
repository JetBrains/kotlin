// WITH_STDLIB

// LANGUAGE: +MultiPlatformProjects
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun foo()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual fun foo() {
    pr<caret>intln()
}
