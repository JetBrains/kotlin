// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

expect interface S1
expect interface S2

open class A : S1, S2

class B : A()

// MODULE: jvm()()(common)
// FILE: main.kt

actual interface S1 {
    fun f() {}
}

actual interface S2 {
    fun f() {}
}
