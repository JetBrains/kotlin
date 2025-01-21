// JVM_DEFAULT_MODE: no-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
interface I {
    fun foo(x: String = "OK"): String = x
}

interface J : I

object O : J

fun box(): String = O.foo()
