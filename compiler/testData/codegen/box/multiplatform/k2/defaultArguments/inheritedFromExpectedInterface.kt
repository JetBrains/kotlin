// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect interface I {
    fun f(p: String = "OK"): String
}

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface I {
    actual fun f(p: String): String
}

class Impl : I {
    override fun f(p: String) = p
}

fun box() = Impl().f()
