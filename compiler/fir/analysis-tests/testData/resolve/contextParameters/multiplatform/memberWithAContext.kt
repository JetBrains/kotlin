// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class A

expect class Member {
    context(a: A)
    fun foo()

    context(a: A)
    val bar: String
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Member {
    context(a: A)
    actual fun foo() { }

    context(a: A)
    actual val bar: String
        get() = ""
}