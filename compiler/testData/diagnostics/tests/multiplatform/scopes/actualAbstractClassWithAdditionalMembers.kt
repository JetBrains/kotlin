// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-64187

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
expect abstract class Base

expect class Derived : Base

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual abstract class Base {
    abstract fun foo()
}

actual class Derived : Base() {
    override fun foo() {}
}
