// TARGET_BACKEND: JVM

// WITH_STDLIB
// FILE: u1.kt

object O {
    @JvmStatic
    fun foo() = "OK"
}

// FILE: u2.kt

fun box() = (O::foo)()
