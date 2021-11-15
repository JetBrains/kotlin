// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
interface I {
    @JvmDefault
    fun foo(x: String = "OK"): String = x
}

interface J : I

object O : J

fun box(): String = O.foo()
