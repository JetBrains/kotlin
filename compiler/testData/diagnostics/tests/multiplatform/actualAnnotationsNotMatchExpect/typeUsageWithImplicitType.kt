// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class Ann

expect fun foo(): @Ann Int

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun foo() = 1
