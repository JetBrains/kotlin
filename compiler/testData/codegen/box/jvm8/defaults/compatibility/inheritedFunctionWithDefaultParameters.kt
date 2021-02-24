// !JVM_DEFAULT_MODE: compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
interface I {
    @JvmDefault
    fun foo(x: String = "OK"): String = x
}

interface J : I

object O : J

fun box(): String = O.foo()
