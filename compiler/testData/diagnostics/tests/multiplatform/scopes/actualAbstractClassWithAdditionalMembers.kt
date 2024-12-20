// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-64187

// MODULE: common
// FILE: common.kt
expect abstract class Base

expect class Derived : Base

// MODULE: jvm()()(common)
// FILE: main.kt

actual abstract class Base {
    abstract fun foo()
}

actual class Derived : Base() {
    override fun foo() {}
}
