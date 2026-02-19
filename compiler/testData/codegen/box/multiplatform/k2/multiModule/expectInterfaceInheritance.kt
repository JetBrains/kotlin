// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// ISSUE: KT-56398

// MODULE: common
// FILE: common.kt

expect interface Interface {
    fun foo(): String
}

class Klass : Interface {
    override fun foo() = "OK"
}

// MODULE: jvm()()(common)
// FILE: main.kt

actual interface Interface {
    actual fun foo(): String
}

fun box() = Klass().foo()
