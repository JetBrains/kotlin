// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: a.kt

object O {
    @JvmStatic
    fun foo() = "OK"
}

// FILE: b.kt
fun box() = (O::foo)()
