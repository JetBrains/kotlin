// TARGET_BACKEND: JVM
// WITH_STDLIB

object Host {
    @JvmStatic
    fun foo(s: String = "OK") = s
}

fun box(): String = Host.foo()