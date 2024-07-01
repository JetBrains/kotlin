// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect interface Base {
    fun foo(a: String): String
}

expect fun buildBase(): Base

class DelegatedImpl(val foo: Base = buildBase()) : Base by foo

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base {
    actual fun foo(a: String): String
}

actual fun buildBase(): Base = Impl()

class Impl : Base {
    override fun foo(a: String): String = "OK"
}

fun box(): String {
    return DelegatedImpl().foo("")
}