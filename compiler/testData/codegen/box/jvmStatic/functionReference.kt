// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: u1.kt

object O {
    @JvmStatic
    fun foo() = "OK"
}

// FILE: u2.kt

fun box() = (O::foo)()
