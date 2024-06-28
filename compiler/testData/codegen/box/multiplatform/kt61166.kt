// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-60854
// WITH_STDLIB
// FULL_JDK

// MODULE: common
// FILE: common.kt
expect object A
abstract class B : C()
expect abstract class C()

// MODULE: intermediate()()(common)
// FILE: intermediate.kt
actual object A : B() {
    override val x: String get() = "OK"
}

actual abstract class C {
    abstract val x: String
}
// MODULE: platform()()(intermediate)
// FILE: platform.kt

fun box(): String = A.x
