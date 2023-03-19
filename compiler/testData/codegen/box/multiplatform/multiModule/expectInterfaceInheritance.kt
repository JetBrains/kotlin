// ISSUE: KT-56398
// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect interface Interface {
    fun foo(): String
}

class Klass : Interface {
    override fun foo() = "OK"
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual interface Interface {
    actual fun foo(): String
}

fun box() = Klass().foo()