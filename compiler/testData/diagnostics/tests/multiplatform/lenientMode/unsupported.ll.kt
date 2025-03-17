// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects
// LENIENT_MODE

// MODULE: common
// FILE: common.kt
expect enum class E {
    Foo, Bar,
}

expect annotation class A

expect value class V(val s: String)

open class C1(s: String)

expect class C2 : C1

// MODULE: jvm()()(common)
// FILE: jvm.kt
fun main() {}