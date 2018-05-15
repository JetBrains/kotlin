// !LANGUAGE: +MultiPlatformProjects

// FILE: lib.kt
expect interface U {
    fun f(p: String = "OK"): String
}

// FILE: main.kt
actual interface U {
    actual fun f(p: String): String
}

interface E: U {
    override fun f(p: String) = p
}

class UU: E {

}

fun box() = UU().f()
