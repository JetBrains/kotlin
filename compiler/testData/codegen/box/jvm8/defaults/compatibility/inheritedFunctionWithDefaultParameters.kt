// !JVM_DEFAULT_MODE: compatibility
// TARGET_BACKEND: JVM

interface I {
    fun foo(x: String = "OK"): String = x
}

interface J : I

object O : J

fun box(): String = O.foo()
