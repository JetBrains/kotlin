// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: default argument mapping in MPP isn't designed yet

// FILE: lib.kt
expect interface I {
    fun f(p: String = "OK"): String
}

// FILE: main.kt
actual interface I {
    actual fun f(p: String): String
}

class Impl : I {
    override fun f(p: String) = p
}

fun box() = Impl().f()
